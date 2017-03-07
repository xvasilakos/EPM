/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exceptions;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class NotIntiliazedException extends Exception {

    public NotIntiliazedException() {
        super();
    }

    public NotIntiliazedException(String msg) {
        super(msg);
    }

    public NotIntiliazedException(StringBuilder msg) {
        super(msg.toString());
    }

    public NotIntiliazedException(Throwable ex) {
        super(ex);
    }

}
