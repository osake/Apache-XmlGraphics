/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: LocalizableSupport.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

// CSOFF: InnerAssignment

/**
 * This class provides a default implementation of the Localizable interface.
 * You can use it as a base class or as a member field and delegates various
 * work to it.
 * <p>
 * For example, to implement Localizable, the following code can be used:
 * 
 * <pre>
 *  package mypackage;
 *  ...
 *  public class MyClass implements Localizable {
 *      // This code fragment requires a file named
 *      // 'mypackage/resources/Messages.properties', or a
 *      // 'mypackage.resources.Messages' class which extends
 *      // java.util.ResourceBundle, accessible using the current
 *      // classpath.
 *      LocalizableSupport localizableSupport =
 *          new LocalizableSupport("mypackage.resources.Messages");
 * 
 *      public void setLocale(Locale l) {
 *          localizableSupport.setLocale(l);
 *      }
 *      public Local getLocale() {
 *          return localizableSupport.getLocale();
 *      }
 *      public String formatMessage(String key, Object[] args) {
 *          return localizableSupport.formatMessage(key, args);
 *      }
 *  }
 * </pre>
 * 
 * The algorithm for the Locale lookup in a LocalizableSupport object is:
 * <ul>
 * <li>
 * if a Locale has been set by a call to setLocale(), use this Locale, else,
 * <li/>
 * <li>
 * if a Locale has been set by a call to the setDefaultLocale() method of a
 * LocalizableSupport object in the current LocaleGroup, use this Locale, else,</li>
 * <li>
 * use the object returned by Locale.getDefault() (and set by
 * Locale.setDefault()).
 * <li/>
 * </ul>
 * This offers the possibility to have a different Locale for each object, a
 * Locale for a group of object and/or a Locale for the JVM instance.
 * <p>
 * Note: if no group is specified a LocalizableSupport object belongs to a
 * default group common to each instance of LocalizableSupport.
 *
 * @version $Id: LocalizableSupport.java 1345683 2012-06-03 14:50:33Z gadams $
 *
 *          Originally authored by Stephane Hillion.
 */
public class LocalizableSupport implements Localizable {
    /**
     * The locale group to which this object belongs.
     */
    protected LocaleGroup localeGroup = LocaleGroup.DEFAULT;

    /**
     * The resource bundle classname.
     */
    protected String bundleName;

    /**
     * The classloader to use to create the resource bundle.
     */
    protected ClassLoader classLoader;

    /**
     * The current locale.
     */
    protected Locale locale;

    /**
     * The locale in use.
     */
    protected Locale usedLocale;

    /**
     * The resources
     */
    protected ResourceBundle resourceBundle;

    /**
     * Same as LocalizableSupport(s, null).
     */
    public LocalizableSupport(final String s) {
        this(s, null);
    }

    /**
     * Creates a new Localizable object. The resource bundle class name is
     * required allows the use of custom classes of resource bundles.
     * 
     * @param s
     *            must be the name of the class to use to get the appropriate
     *            resource bundle given the current locale.
     * @param cl
     *            is the classloader used to create the resource bundle, or
     *            null.
     * @see java.util.ResourceBundle
     */
    public LocalizableSupport(final String s, final ClassLoader cl) {
        this.bundleName = s;
        this.classLoader = cl;
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.Localizable#setLocale(Locale)}.
     */
    @Override
    public void setLocale(final Locale l) {
        if (this.locale != l) {
            this.locale = l;
            this.resourceBundle = null;
        }
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.Localizable#getLocale()}.
     */
    @Override
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.ExtendedLocalizable#setLocaleGroup(LocaleGroup)}
     * .
     */
    public void setLocaleGroup(final LocaleGroup lg) {
        this.localeGroup = lg;
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.ExtendedLocalizable#getLocaleGroup()}
     * .
     */
    public LocaleGroup getLocaleGroup() {
        return this.localeGroup;
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.ExtendedLocalizable#setDefaultLocale(Locale)}
     * . Later invocations of the instance methods will lead to update the
     * resource bundle used.
     */
    public void setDefaultLocale(final Locale l) {
        this.localeGroup.setLocale(l);
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.ExtendedLocalizable#getDefaultLocale()}
     * .
     */
    public Locale getDefaultLocale() {
        return this.localeGroup.getLocale();
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.Localizable#formatMessage(String,Object[])}
     * .
     */
    @Override
    public String formatMessage(final String key, final Object[] args) {
        getResourceBundle();
        return MessageFormat.format(this.resourceBundle.getString(key), args);
    }

    /**
     * Implements
     * {@link org.apache.xmlgraphics.util.i18n.ExtendedLocalizable#getResourceBundle()}
     * .
     */
    public ResourceBundle getResourceBundle() {
        Locale l;

        if (this.resourceBundle == null) {
            if (this.locale == null) {
                if ((l = this.localeGroup.getLocale()) == null) {
                    this.usedLocale = Locale.getDefault();
                } else {
                    this.usedLocale = l;
                }
            } else {
                this.usedLocale = this.locale;
            }
            if (this.classLoader == null) {
                this.resourceBundle = ResourceBundle.getBundle(this.bundleName,
                        this.usedLocale);
            } else {
                this.resourceBundle = ResourceBundle.getBundle(this.bundleName,
                        this.usedLocale, this.classLoader);
            }
        } else if (this.locale == null) {
            // Check for group Locale and JVM default locale changes.
            if ((l = this.localeGroup.getLocale()) == null) {
                if (this.usedLocale != (l = Locale.getDefault())) {
                    this.usedLocale = l;
                    if (this.classLoader == null) {
                        this.resourceBundle = ResourceBundle.getBundle(
                                this.bundleName, this.usedLocale);
                    } else {
                        this.resourceBundle = ResourceBundle.getBundle(
                                this.bundleName, this.usedLocale,
                                this.classLoader);
                    }
                }
            } else if (this.usedLocale != l) {
                this.usedLocale = l;
                if (this.classLoader == null) {
                    this.resourceBundle = ResourceBundle.getBundle(
                            this.bundleName, this.usedLocale);
                } else {
                    this.resourceBundle = ResourceBundle.getBundle(
                            this.bundleName, this.usedLocale, this.classLoader);
                }
            }
        }

        return this.resourceBundle;
    }
}
