package de.dlr.ivf.tapas.environment.gui.fx.viewmodel.util;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeUtils {

    public static DateTimeFormatter dateTimeFormatter(){
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public static String durationToFormattedTime(Duration duration){
        return String.format("%d:%02d:%02d",duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
