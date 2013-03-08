/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/12/17
 * $Id:: StringPatternValidator.java 9616 2012-12-30 23:23:22Z pdias      $:
 */
package pt.up.fe.dceg.neptus.util.conf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pdias
 * 
 */
public class StringPatternValidator implements Validator {

    protected String redex = "(\\w(((\\s)*)?,((\\s)*)?)?)*";

    public StringPatternValidator() {
    }

    public StringPatternValidator(String redex) {
        this.redex = redex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.util.conf.Validator#validate(java.lang.Object)
     */
    public String validate(Object newValue) {
        try {
            String comp = (String) newValue;

            Pattern p = Pattern.compile(redex);
            Matcher m = p.matcher(comp);
            boolean b = m.matches();
            return (b) ? null : ("The value '" + comp + "' should be in the form '" + redex + "'");
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String validValuesDesc() {
        String ret = "The value should match redex " + redex;
        return ret;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        StringPatternValidator t = new StringPatternValidator();
        System.out.println("ok: " + t.validate("Main"));
        System.out.println("ok: " + t.validate(""));
        System.out.println("ok: " + t.validate("Main,W1"));
        System.out.println("ok: " + t.validate("Main, W1"));
        System.out.println("ok: " + t.validate("Main, W1 ,Main , W1"));
        System.out.println("ok: " + t.validate("Main, W1 ,Main , W1,"));
        System.out.println("nok: " + t.validate("Main:, W1 ,Main , W1"));
        System.out.println("nok: " + t.validate(",Main, W1 ,Main , W1"));
    }

}
