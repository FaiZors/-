package com.sandenderman.entity.goal;

import com.sandenderman.entity.SandEndermanEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class SandBossAttackGoal extends Goal {

    private final SandEndermanEntity boss;
    private PlayerEntity target;
    private int cooldown = 0;
    private int normalTeleportCooldown = 0;

    public SandBossAttackGoal(SandEndermanEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return boss.getTarget() instanceof PlayerEntity;
    }

    @Override
    public boolean shouldContinue() {
        return boss.getTarget() instanceof PlayerEntity;
    }

    @Override
    public void start() {
        target = (PlayerEntity) boss.getTarget();
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            target = (PlayerEntity) boss.getTarget();
            if (target == null) return;
        }

        boss.getLookControl().lookAt(target, 100.0f, 100.0f);

        if (cooldown > 0) cooldown--;
        if (normalTeleportCooldown > 0) normalTeleportCooldown--;

        boolean enraged = boss.isEnraged();

        if (!enraged) {
            handleNormalStage();
        } else {
            handleRageStageAttack();
        }
    }

    private void handleNormalStage() {
        boolean playerLooking = isPlayerLooking();

        if (!playerLooking) {
            if (normalTeleportCooldown <= 0) {
                teleportToPlayer();
                normalTeleportCooldown = 60;
            }

            double dist = boss.squaredDistanceTo(target);
            if (dist <= 9.0 && cooldown <= 0) {
                boss.tryAttack(target);
                cooldown = 30;
            } else if (dist > 9.0) {
                boss.getNavigation().startMovingTo(target, 1.0);
            }
        }
    }

    private void handleRageStageAttack() {
        double dist = boss.squaredDistanceTo(target);
        if (dist <= 9.0 && cooldown <= 0) {
            boss.tryAttack(target);
            cooldown = 25;
        } else if (dist > 9.0) {
            boss.getNavigation().startMovingTo(target, 1.2);
        }
    }

    private void teleportToPlayer() {
        Vec3d targetPos = target.getPos();
        boss.teleport(targetPos.x, targetPos.y, targetPos.z);
        boss.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private boolean isPlayerLooking() {
        if (target == null) return false;
        Vec3d eyePos = target.getEyePos();
        Vec3d lookVec = target.getRotationVec(1.0f).normalize();
        Vec3d farPos = eyePos.add(lookVec.multiply(64.0));
        var hit = boss.getBoundingBox().expand(0.2).raycast(eyePos, farPos);
        return hit.isPresent();
    }
}
