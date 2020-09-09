// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.climateConditions;

import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.delay.DelayManager;
import org.terasology.engine.logic.delay.PeriodicActionTriggeredEvent;
import org.terasology.engine.logic.location.Location;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.JomlUtil;
import org.terasology.engine.particles.components.generators.VelocityRangeGeneratorComponent;
import org.terasology.engine.registry.In;
import org.terasology.math.geom.Vector3f;

/**
 * Adds a {@link VisibleBreathComponent} to the player. Visible breath is a periodic action adding a visible breath
 * particle effect. Is active only iff the player has a {@link HypothermiaComponent}
 */
@RegisterSystem(value = RegisterMode.AUTHORITY)
public class VisibleBreathingSystem extends BaseComponentSystem {
    public static final String VISIBLE_BREATH_ACTION_ID = "Visible Breath";
    private final int initialDelay = 5000;
    private final int breathInterval = 7000;
    @In
    private EntityManager entityManager;
    @In
    private DelayManager delayManager;

    @ReceiveEvent(components = {HypothermiaComponent.class})
    public void onHypothermia(OnAddedComponent event, EntityRef player) {
        delayManager.addPeriodicAction(player, VisibleBreathingSystem.VISIBLE_BREATH_ACTION_ID, initialDelay,
                breathInterval);
    }

    @ReceiveEvent(components = HypothermiaComponent.class)
    public void beforeRemoveHypothermia(BeforeRemoveComponent event, EntityRef player) {
        delayManager.cancelPeriodicAction(player, VisibleBreathingSystem.VISIBLE_BREATH_ACTION_ID);
    }

    @ReceiveEvent(components = {HypothermiaComponent.class})
    public void onPeriodicBreath(PeriodicActionTriggeredEvent event, EntityRef player, LocationComponent location) {
        if (event.getActionId().equals(VISIBLE_BREATH_ACTION_ID)) {
            updateVisibleBreathEffect(player, location);
        }
    }

    private void updateVisibleBreathEffect(EntityRef player, LocationComponent targetLoc) {
        EntityRef particleEntity = entityManager.create("climateConditions:VisibleBreathEffect");
        LocationComponent childLoc = particleEntity.getComponent(LocationComponent.class);
        childLoc.setWorldPosition(targetLoc.getWorldPosition());
        Location.attachChild(player, particleEntity);
        particleEntity.setOwner(player);
        Vector3f direction = targetLoc.getLocalDirection();
        direction.normalize();
        particleEntity.upsertComponent((VelocityRangeGeneratorComponent.class), maybeComponent -> {
            VelocityRangeGeneratorComponent velocity = maybeComponent.orElse(new VelocityRangeGeneratorComponent());
            direction.scale(0.5f);
            direction.addY(0.5f);
            velocity.minVelocity = JomlUtil.from(direction);
            direction.scale(1.5f);
            velocity.maxVelocity = JomlUtil.from(direction);
            return velocity;
        });
        player.upsertComponent((VisibleBreathComponent.class), maybeComponent -> {
            VisibleBreathComponent component = maybeComponent.orElse(new VisibleBreathComponent());
            component.particleEntity = particleEntity;
            return component;
        });
    }
}
