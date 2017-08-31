package pt.lsts.neptus.mra.plots.filters;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MraFilterDescription {
    String name();
    String abbrev();
    String author() default "LSTS-FEUP";
    String description();
}
