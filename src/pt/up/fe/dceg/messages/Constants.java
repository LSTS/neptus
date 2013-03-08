// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 
package pt.up.fe.dceg.messages;

/**
 * Constants used by messaging classes.
 * @author Eduardo 
 */
public interface Constants {
    /**
     * Null serial id.
     * It is used to mark null inline messages
     */
    public static final int NULL_SERIAL_ID=0xFFFF;

    /**
     * Min value for UINT8 type.
     */
    public static final short MIN_UINT8 = 0;

    /**
     * Min value for UINT16 type.
     */
    public static final int MIN_UINT16 = 0;

    /**
     * Min value for UINT32 type.
     */
    public static final int MIN_UINT32 = 0;

    /**
     * Min value for UINT64 type.
     */
    public static final long MIN_UINT64 = 0L;

    /**
     * Min value for INT8 type.
     */
    public static final short MIN_INT8 = Byte.MIN_VALUE;

    /**
     * Min value for INT16 type.
     */
    public static final short MIN_INT16 = Short.MIN_VALUE;

    /**
     * Min value for INT32 type.
     */
    public static final int MIN_INT32 = Integer.MIN_VALUE;

    /**
     * Min value for INT64 type.
     */
    public static final long MIN_INT64 = Long.MIN_VALUE;

    /**
     * Max value for INT8 type.
     */
    public static final short MAX_INT8 = Byte.MAX_VALUE;

    /**
     * Max value for INT16 type.
     */
    public static final short MAX_INT16 = Short.MAX_VALUE;

    /**
     * Max value for INT32 type.
     */
    public static final int MAX_INT32 = Integer.MAX_VALUE;

    /**
     * Max value for INT64 type.
     */
    public static final long MAX_INT64 = Long.MAX_VALUE;

    /**
     * Max value for UINT8 type.
     */
    public static final short MAX_UINT8 = 255;

    /**
     * Max value for UINT16 type.
     */
    public static final int MAX_UINT16 = Character.MAX_VALUE;

    /**
     * Max value for UINT32 type.
     */
    public static final long MAX_UINT32 = (1L << 32) - 1;

    /**
     * Max value for UINT64 type (not accurate)
     */
    public static final long MAX_UINT64 = Long.MAX_VALUE;

    /**
     * Min value for FP32 type.
     */
    public static final float MIN_FP32 = Float.MIN_VALUE;

    /**
     * Max value for FP32 type.
     */
    public static final float MAX_FP32 = Float.MAX_VALUE;

    /**
     * Min value for FP64 type.
     */
    public static final double MIN_FP64 = Double.MIN_VALUE;

    /**
     * Max value for FP64 type.
     */
    public static final double MAX_FP64 = Double.MAX_VALUE;
}
