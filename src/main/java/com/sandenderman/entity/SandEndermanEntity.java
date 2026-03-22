package com.sandenderman.entity;

import com.sandenderman.entity.goal.SandBossAttackGoal;
import com.sandenderman.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class SandEndermanEntity extends HostileEntity {

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.literal("Песчаный Эндермен"),
            BossBar.Color.YELLOW,
            BossBar.Style.PROGRESS
    );

    private boolean enraged = false;
    private boolean frozen = false;

    private int rageTeleportTimer = 0;
    private int rageFreezeTimer = 0;
    private int throwCooldown = 0;
    private int particleTimer = 0;

    private static final float ENRAGE_HP_THRESHOLD = 0.2f;
    private static final float NORMAL_DAMAGE = 9.0f;
    private static final float RAGE_DAMAGE = 21.0f;
    private static final float REFLECT_PERCENT = 0.40f;
    private static final double RAGE_TELEPORT_RADIUS = 4.0;
    private static final int RAGE_TELEPORT_INTERVAL = 20;

    private static final Vector3f SAND_COLOR = new Vector3f(0.80f, 0.64f, 0.32f);
    private static final DustParticleEffect SAND_PARTICLE = new DustParticleEffect(SAND_COLOR, 1.2f);

    public SandEndermanEntity(EntityType<? extends SandEndermanEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 200;
    }

    public static DefaultAttributeContainer.Builder createSandEndermanAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 400.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SandBossAttackGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.5, 0.5f));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));

        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            handleServerTick();
        } else {
            handleClientParticles();
        }
    }

    private void handleServerTick() {
        bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        if (!enraged && this.getHealth() <= this.getMaxHealth() * ENRAGE_HP_THRESHOLD) {
            enraged = true;
            bossBar.setColor(BossBar.Color.RED);
            bossBar.setName(Text.literal("☠ Песчаный Эндермен [ЯРОСТЬ] ☠"));
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                    .setBaseValue(RAGE_DAMAGE);
            this.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.7f);
        }

        PlayerEntity target = this.getTarget() instanceof PlayerEntity p ? p : null;

        spawnSandParticlesServer();

        if (enraged) {
            handleRageStage(target);
        } else {
            handleNormalStage(target);
        }
    }

    private void handleNormalStage(PlayerEntity target) {
        if (target == null) return;

        boolean playerLooking = isPlayerLookingAtBoss(target);
        this.frozen = playerLooking;

        if (playerLooking) {
            this.getNavigation().stop();
            this.setVelocity(0, this.getVelocity().y, 0);
        }
    }

    private void handleRageStage(PlayerEntity target) {
        if (target == null) return;

        frozen = false;

        if (rageFreezeTimer > 0) {
            rageFreezeTimer--;
            this.getNavigation().stop();
            return;
        }

        rageTeleportTimer--;
        if (rageTeleportTimer <= 0) {
            rageTeleportTimer = RAGE_TELEPORT_INTERVAL;
            teleportAroundPlayer(target);

            if (this.getRandom().nextFloat() < 0.75f) {
                rageFreezeTimer = 20;
            }
        }

        if (throwCooldown > 0) throwCooldown--;
    }

    private void teleportAroundPlayer(PlayerEntity player) {
        double angle = this.getRandom().nextDouble() * Math.PI * 2;
        double radius = 1.5 + this.getRandom().nextDouble() * (RAGE_TELEPORT_RADIUS - 1.5);
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;

        double targetX = player.getX() + dx;
        double targetY = player.getY();
        double targetZ = player.getZ() + dz;

        BlockPos targetPos = new BlockPos((int) targetX, (int) targetY, (int) targetZ);
        if (!this.getWorld().getBlockState(targetPos).isSolid()
                && !this.getWorld().getBlockState(targetPos.down()).isAir()) {
            this.teleport(targetX, targetY, targetZ);
            this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (!result) return false;

        if (!(target instanceof PlayerEntity player)) return true;

        if (enraged) {
            if (this.getRandom().nextFloat() < 0.60f) {
                StatusEffectInstance effect = this.getRandom().nextBoolean()
                        ? new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0)
                        : new StatusEffectInstance(StatusEffects.WITHER, 100, 1);
                player.addStatusEffect(effect);
            }

            if (throwCooldown <= 0 && this.getRandom().nextFloat() < 0.25f) {
                throwPlayerUp(player);
                throwCooldown = 200;
            }
        }

        return true;
    }

    private void throwPlayerUp(PlayerEntity player) {
        player.setVelocity(player.getVelocity().x, 2.85, player.getVelocity().z);
        player.velocityModified = true;
        player.fallDistance = 0;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();

        if (!enraged && frozen && attacker instanceof PlayerEntity player) {
            float reflectDamage = amount * REFLECT_PERCENT;
            player.damage(player.getDamageSources().magic(), reflectDamage);
        }

        return super.damage(source, amount);
    }

    private boolean isPlayerLookingAtBoss(PlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0f).normalize();
        Vec3d farPos = eyePos.add(lookVec.multiply(64.0));

        Box bossBox = this.getBoundingBox().expand(0.2);
        Optional<Vec3d> hit = bossBox.raycast(eyePos, farPos);
        return hit.isPresent();
    }

    private void spawnSandParticlesServer() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        particleTimer++;
        if (particleTimer % 2 != 0) return;

        double x = this.getX() + (this.getRandom().nextDouble() - 0.5) * 1.2;
        double y = this.getY() + this.getRandom().nextDouble() * this.getHeight();
        double z = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 1.2;

        double vx = (this.getRandom().nextDouble() - 0.5) * 0.3;
        double vy = this.getRandom().nextDouble() * 0.2;
        double vz = (this.getRandom().nextDouble() - 0.5) * 0.3;

        serverWorld.spawnParticles(SAND_PARTICLE, x, y, z, 3, vx, vy, vz, 0.05);
    }

    private void handleClientParticles() {
        particleTimer++;
        if (particleTimer % 2 != 0) return;

        for (int i = 0; i < 3; i++) {
            double x = this.getX() + (this.getRandom().nextDouble() - 0.5) * 1.2;
            double y = this.getY() + this.getRandom().nextDouble() * this.getHeight();
            double z = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 1.2;
            this.getWorld().addParticle(SAND_PARTICLE, x, y, z,
                    (this.getRandom().nextDouble() - 0.5) * 0.2,
                    this.getRandom().nextDouble() * 0.15,
                    (this.getRandom().nextDouble() - 0.5) * 0.2);
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);

        int soulCount = 1 + this.getRandom().nextInt(2);
        this.dropStack(new ItemStack(ModItems.DESERT_SOUL, soulCount));

        int fragmentCount = 3 + this.getRandom().nextInt(4);
        this.dropStack(new ItemStack(ModItems.SAND_FRAGMENT, fragmentCount));

        if (this.getRandom().nextFloat() < 0.20f) {
            this.dropStack(new ItemStack(ModItems.CURSED_DESERT_EYE, 1));
        }
    }

    public boolean isBossCurrentlyFrozen() {
        return frozen && !enraged;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Enraged", enraged);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        enraged = nbt.getBoolean("Enraged");
        if (enraged) {
            bossBar.setColor(BossBar.Color.RED);
            bossBar.setName(Text.literal("☠ Песчаный Эндермен [ЯРОСТЬ] ☠"));
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                    .setBaseValue(RAGE_DAMAGE);
        }
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    protected boolean canTeleport() {
        return !frozen;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public boolean isBossFrozen() {
        return frozen && !enraged;
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                  SpawnReason spawnReason, @Nullable EntityData entityData,
                                  @Nullable NbtCompound entityNbt) {
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }
}
