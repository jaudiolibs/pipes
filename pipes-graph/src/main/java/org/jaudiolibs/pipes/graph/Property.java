/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 */
package org.jaudiolibs.pipes.graph;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * A wrapper type for double values that supports key frame animation in sync
 * with the {@link Graph}. This type partly parallels the same named type in the
 * PraxisLIVE (audio) API.
 * <p>
 * Property fields may be annotated with {@link Inject}.
 */
public final class Property implements Graph.Dependent {

    private static final long TO_NANO = 1000_000_000;

    private final List<Link> links;

    private Animator animator;
    private double value;
    private Graph graph;

    public Property() {
        this.links = new CopyOnWriteArrayList<>();
    }

    /**
     * Set the value of this property. This will cancel any active animation,
     * and notify any attached links.
     *
     * @param value
     * @return this
     */
    public Property set(double value) {
        finishAnimating();
        setImpl(value);
        return this;
    }

    /**
     * Return the current value.
     *
     * @return value
     */
    public double get() {
        return value;
    }

    /**
     * Call the provided consumer with the double value whenever the value
     * changes. This is a shorthand for {@code values().link(consumer);}. The
     * double value will be as if calling {@link #getDouble()}.
     *
     * @param consumer double consumer
     * @return this
     */
    public Property link(DoubleConsumer consumer) {
        Link dl = new Link();
        dl.link(consumer);
        return this;
    }

    /**
     * Return a new {@link Linkable.Double} for observing changing values. The
     * double value will be as if calling {@link #get()}.
     *
     * @return Linkable.Double of values.
     */
    public Linkable.Double values() {
        return new Link();
    }

    /**
     * Clear all Linkables from the Property.
     *
     * @return this
     */
    public Property clearLinks() {
        links.clear();
        return this;
    }

    /**
     * Return the Animator for the Property, creating it if necessary.
     *
     * @return property animator
     */
    public Animator animator() {
        if (animator == null) {
            animator = new Animator(this);
        }
        return animator;
    }

    /**
     * Animate the property value to the provided values. This is a shorthand
     * for calling {@code animator().to(...)}.
     * <p>
     * This method returns the animator so that you can chain calls, eg.
     * {@code prop.to(1, 0).in(1, 0.25).easeInOut();}
     *
     * @return property animator
     */
    public Property.Animator to(double... to) {
        return animator().to(to);
    }

    @Override
    public void attach(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void detach(Graph graph) {
        finishAnimating();
        this.graph = null;
    }

    @Override
    public void update() {
        if (animator != null) {
            animator.tick();
        }
    }

    /**
     * Return whether the property is currently animating.
     *
     * @return property animator active
     */
    public boolean isAnimating() {
        return animator != null && animator.isAnimating();
    }

    private void setImpl(double value) {
        this.value = value;
        updateLinks();
    }

    private void finishAnimating() {
        if (animator != null) {
            animator.stop();
        }
    }

    private void updateLinks() {
        links.forEach(l -> l.fire(value));
    }

    private class Link implements Linkable.Double {

        private DoubleConsumer consumer;

        @Override
        public void link(DoubleConsumer consumer) {
            if (this.consumer != null) {
                throw new IllegalStateException("Cannot link multiple consumers in one chain");
            }
            this.consumer = Objects.requireNonNull(consumer);
            fire(get());
            links.add(this);
        }

        private void fire(double value) {
            consumer.accept(value);
        }

    }

    /**
     * Provides keyframe animation support for Property. Methods return this so
     * that they can be chained - eg. {@code to(1, 0).in(1, 0.25).easeInOut()}
     */
    public static class Animator {

        private final static long[] DEFAULT_IN = new long[]{0};
        private final static Easing[] DEFAULT_EASING = new Easing[]{Easing.linear};

        private Property property;
        int index;
        private double[] to;
        private long[] in;
        private Easing[] easing;

        private double fromValue;
        private long fromTime;
        private boolean animating;

        private Consumer<Property> onDoneConsumer;
        private long overrun;

        private Animator(Property p) {
            this.property = p;
            in = DEFAULT_IN;
            easing = DEFAULT_EASING;
        }

        private void attach(Property p) {
            this.property = p;
        }

        /**
         * Set the target values for animation and start animation. The number
         * of values provided to this method controls the number of keyframes.
         *
         * @param to target values
         * @return this
         */
        public Animator to(double... to) {
            if (to.length < 1) {
                throw new IllegalArgumentException();
            }
            this.to = to;
            index = 0;
            in = DEFAULT_IN;
            easing = DEFAULT_EASING;
            fromValue = property.value;
            fromTime = property.graph.nanos();
            if (!animating) {
                animating = true;
            }
            return this;
        }

        /**
         * Set the time in seconds for each keyframe. The number of provided
         * values may be different than the number of keyframes passed to to().
         * Values will be cycled through as needed.
         * <p>
         * eg. {@code to(100, 50, 250).in(1, 0.5)} is the same as
         * {@code to(100, 50, 250).in(1, 0.5, 1)}
         *
         * @param in times in seconds
         * @return this
         */
        public Animator in(double... in) {
            if (in.length < 1) {
                this.in = DEFAULT_IN;
            } else {
                this.in = new long[in.length];
                for (int i = 0; i < in.length; i++) {
                    this.in[i] = (long) (in[i] * TO_NANO);
                }
                this.in[0] = Math.max(0, this.in[0] - overrun);
            }
            return this;
        }

        /**
         * Set the easing mode for each keyframe. The number of provided values
         * may be different than the number of keyframes passed to to(). Values
         * will be cycled through as needed.
         *
         * @param easing easing mode to use
         * @return this
         */
        public Animator easing(Easing... easing) {
            if (easing.length < 1) {
                this.easing = DEFAULT_EASING;
            } else {
                this.easing = easing;
            }
            return this;
        }

        /**
         * Convenience method to use {@link Easing#linear} easing for all
         * keyframes.
         *
         * @return this
         */
        public Animator linear() {
            return easing(Easing.linear);
        }

        /**
         * Convenience method to use {@link Easing#ease} easing for all
         * keyframes.
         *
         * @return this
         */
        public Animator ease() {
            return easing(Easing.ease);
        }

        /**
         * Convenience method to use {@link Easing#easeIn} easing for all
         * keyframes.
         *
         * @return this
         */
        public Animator easeIn() {
            return easing(Easing.easeIn);
        }

        /**
         * Convenience method to use {@link Easing#easeOut} easing for all
         * keyframes.
         *
         * @return this
         */
        public Animator easeOut() {
            return easing(Easing.easeOut);
        }

        /**
         * Convenience method to use {@link Easing#easeInOut} easing for all
         * keyframes.
         *
         * @return this
         */
        public Animator easeInOut() {
            return easing(Easing.easeInOut);
        }

        /**
         * Stop animating. The current property value will be retained.
         *
         * @return this
         */
        public Animator stop() {
            index = 0;
            animating = false;
            return this;
        }

        /**
         * Whether an animation is currently active.
         *
         * @return animation active
         */
        public boolean isAnimating() {
            return animating;
        }

        /**
         * Set a consumer to be called each time the Animator finishes
         * animation. Also calls the consumer immediately if no animation is
         * currently active.
         * <p>
         * Unlike restarting an animation by polling isAnimating(), an animation
         * started inside this consumer will take into account any time overrun
         * between the target and actual finish time of the completing
         * animation.
         *
         * @param whenDoneConsumer function to call
         * @return this
         */
        public Animator whenDone(Consumer<Property> whenDoneConsumer) {
            this.onDoneConsumer = whenDoneConsumer;
            if (!animating) {
                onDoneConsumer.accept(property);
            }
            return this;
        }

        private void tick() {
            if (!animating) {
                assert false;
                return;
            }
            try {
                long currentTime = property.graph.nanos();
                double toValue = to[index];
                long duration = in[index % in.length];
                double proportion;
                if (duration < 1) {
                    proportion = 1;
                } else {
                    proportion = (currentTime - fromTime) / (double) duration;
                }
                if (proportion >= 1) {
                    index++;
                    if (index >= to.length) {
                        finish(currentTime - (fromTime + duration));
                    } else {
                        fromValue = toValue;
                        fromTime += duration;
                    }
                    property.setImpl(toValue);
                } else if (proportion > 0) {
                    Easing ease = easing[index % easing.length];
                    double d = ease.calculate(proportion);
                    d = (d * (toValue - fromValue)) + fromValue;
                    property.setImpl(d);
                } else {
//                    p.setImpl(fromTime, fromValue); ???
                }
            } catch (Exception exception) {
                finish(0);
            }

        }

        private void finish(long overrun) {
            index = 0;
            animating = false;
            this.overrun = overrun;
            if (onDoneConsumer != null) {
                onDoneConsumer.accept(property);
            }
            this.overrun = 0;
        }

    }

}
