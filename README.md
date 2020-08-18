Pipes
=====

Pipes is an audio routing and unit generator library linking together the
various other JAudioLibs libraries. Pipes consists of code that was originally
written for or included in [PraxisLIVE](https://www.praxislive.org), but is now
provided separately for re-use by other Java audio or DSP projects.

It currently consists of four modules -

- Pipes Core : the base routing and unit generator API
- Pipes Units : a range of useful audio units, including oscillators, sample
players / loopers, audio effects, etc.
- Pipes Units JavaSound : audio file loading support for Pipes Units based on
JavaSound APIs.
- Pipes Graph : support for creating and playing `Graph` subclasses, providing
many useful additions from the custom audio API inside PraxisLIVE, including a
simplified API for linking units, easy wrapping of lambda functions, audio rate
based animators and `ScheduledExecutorService`, and optional support for
annotation based field injection (allowing easy transfer of code from PraxisCORE).

All modules are licensed under LGPLv3, except for Pipes Units JavaSound which is
licensed under GPLv2+CPE.

## Getting Started

Pipes Graphs makes it easy to get started with building Java audio DSP graphs in
Java, making full use of Java 8+ features.

eg. A simple graph example showing monophonic synth, custom lambda function
to generate noise, and animated filter frequency -

```java
public class SimpleGraph extends Graph {

    @UGen IIRFilter filter;
    @UGen Osc osc;
    @UGen Chorus chorus;

    @Inject Property sweep, env;
    @Inject Clock clock;

    @Override
    protected void init() {
        Pipe noise = link(
                fn(d -> Math.random() * 2 - 1),
                filter.frequency(110).resonance(15),
                chorus.depth(1.4).feedback(0.4).rate(8),
                tee());
        Pipe syn = link(
                osc.waveform(Waveform.Square).gain(0.),
                tee());
        
        link(add(noise, syn), out(0));
        link(add(noise, syn), out(1));

        sweep.animator().whenDone(p -> p.to(8000, 65).in(4, 0.2).easeInOut());
        sweep.link(filter::frequency);

        String[] notes = {"a2", "g2", "d2", "a3", "c#3", "e3"};

        clock.bpm(120).on()
                .filter(i -> i % 8 < 7)
                .mapTo(i -> notes[i % notes.length])
                .link(n -> {
                    env.set(0.8).to(0).in(1);
                    osc.frequency(noteToFrequency(n));
                });

        env.link(d -> osc.gain(d * d * d * d));

    }

    public static void main(String[] args) {
        GraphPlayer.create(new SimpleGraph())
//                .library("JACK")
//                .ext(new ClientID("Simple Graph"))
//                .ext(Connections.ALL)
                .build()
                .start();
    }

}
```

The Pipes Graph library mirrors much of the audio coding API inside PraxisLIVE,
so much of the documentation on
[coding audio components](https://docs.praxislive.org/coding-audio/) is directly
transferable. Also see the JavaDoc and the 
[examples repository](https://github.com/jaudiolibs/examples) for more, or
experiment inside PraxisLIVE itself.
