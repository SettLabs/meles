package io.hardware.gpio;

import com.diozero.api.*;
import com.diozero.sbc.LocalSystemInfo;
import meles.Paths;
import org.tinylog.Logger;

import java.util.Optional;

public class GpioFab {

    public static DigitalInputDevice buildInput(String name, String pull, String edge) {
        var board = new CustomBoard(LocalSystemInfo.getInstance(), Paths.settings());

        var pinInfo = board.getByName(name);
        if (pinInfo == null) {
            Logger.error("Couldn't find match for pin name <" + name + ">");
            return null;
        }

        Logger.info("Matched " + name + " to " + pinInfo.getName());
        return initInput(pinInfo, pull, edge);
    }

    public static DigitalOutputDevice buildOutput(String name, boolean activeHigh, boolean initValue) {
        var board = new CustomBoard(LocalSystemInfo.getInstance(), Paths.settings());

        var pinInfo = board.getByName(name);
        if (pinInfo == null) {
            Logger.error("Couldn't find match for pin name <" + name + ">");
            return null;
        }

        Logger.info("Matched " + name + " to " + pinInfo.getName());
        return addOutput(pinInfo, activeHigh,initValue).orElse(null);
    }

    private static DigitalInputDevice initInput(PinInfo pinInfo, String pull, String edge) {
        /*
        GpioEventTrigger trigger = switch (edge) {
            case "falling" -> GpioEventTrigger.FALLING;
            case "rising" -> GpioEventTrigger.RISING;
            case "both" -> GpioEventTrigger.BOTH;
            default -> GpioEventTrigger.NONE;
        };*/
        GpioEventTrigger trigger = edge.isEmpty()?GpioEventTrigger.NONE:GpioEventTrigger.BOTH;
        GpioPullUpDown pud = switch (pull) {
            case "up" -> GpioPullUpDown.PULL_UP;
            case "down" -> GpioPullUpDown.PULL_DOWN;
            default -> GpioPullUpDown.NONE;
        };

        return addInput(pinInfo, trigger, pud).orElse(null);
    }

    private static Optional<DigitalInputDevice> addInput(PinInfo gpio, GpioEventTrigger event, GpioPullUpDown pud) {
        try {
            var input = DigitalInputDevice.Builder.builder(gpio).setTrigger(event).setPullUpDown(pud).build();
            Logger.info("Setting interruptGpio ({},{}) consumer", input.getGpio(), gpio.getName());
            Logger.info( "Build Result: Pull="+input.getPullUpDown() + " Trigger=" + input.getTrigger() + " Value=" + input.getValue());
            return Optional.of(input);
        } catch (RuntimeIOException e) {
            Logger.error(e);
            Logger.info("Failed to add " + gpio.getName() + " as interrupt");
            return Optional.empty();
        }
    }

    private static Optional<DigitalOutputDevice> addOutput(PinInfo gpio, boolean activeHigh, boolean initValue) {
        try {
            var output = DigitalOutputDevice.Builder.builder(gpio).setActiveHigh(activeHigh).setInitialValue(initValue).build();
            Logger.info("Adding outputpin ({},{})", output.getGpio(), gpio.getName());
            return Optional.of(output);
        } catch (RuntimeIOException e) {
            Logger.error(e);
            Logger.info("Failed to add " + gpio.getName() + " as output");
            return Optional.empty();
        }
    }
    private static Optional<PwmOutputDevice> addPWM(PinInfo gpio, int frequency) {
        try {
            var output = PwmOutputDevice.Builder.builder(gpio).setPwmFrequency(frequency).build();
            Logger.info("Adding PWMoutput ({},{})", output.getGpio(), gpio.getName());
            return Optional.of(output);
        } catch (RuntimeIOException e) {
            Logger.error(e);
            Logger.info("Failed to add " + gpio.getName() + " as output");
            return Optional.empty();
        }
    }
}
