package org.apache.spark.annotation;

import java.lang.annotation.*;

/**
 * An experimental user-facing API.
 *
 * Experimental API's might change or be removed, or be adopted.
 *
 * NOTE: If there exists a Scaladoc comment that immediately precedes this annotation, the first
 * line of the comment must be ":: Experimental ::" with no trailing blank line. This is because
 * of the known issue that Scaladoc displays only either the annotation or the comment, whichever
 * comes first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE, ElementType.PACKAGE})
public @interface Experimental {}
