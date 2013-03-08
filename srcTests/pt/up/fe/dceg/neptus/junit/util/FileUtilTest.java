/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 26/Fev/2005
 * $Id:: FileUtilTest.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.junit.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author Paulo
 *
 */
public class FileUtilTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(FileUtilTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testRelativizeFilePath() throws IOException
    {
        String pathP, path, resC, res;
        //String fxSep = System.getProperty("file.separator", "/");
        
        pathP = "/home/neptus/t.txt";
        path  = "/home/neptus/d1/sign.txt";
        resC  = "d1/sign.txt";
        res   = FileUtil.relativizeFilePath(pathP, path);
        assertEquals(new File(resC).getPath(), res);
        
        pathP = "/home/neptus/info/t.txt";
        path  = "/home/d1/sign.txt";
        resC  = "../../d1/sign.txt";
        res   = FileUtil.relativizeFilePath(pathP, path);
        assertEquals(new File(resC).getPath(), res);
        
        pathP = "/home/neptus/info.txt";
        path  = "/e/d1/sign.txt";
        resC  = "/e/d1/sign.txt";
        res   = FileUtil.relativizeFilePath(pathP, path);
        assertEquals(new File(resC).getCanonicalPath(), res);
        
        pathP = "/home/neptus/miss.txt";
        path  = "/home/neptus/sign.txt";
        resC  = "sign.txt";
        res   = FileUtil.relativizeFilePath(pathP, path);
        assertEquals(new File(resC).getPath(), res);
        
        File curDir = new File(".");
        String absCurPath = curDir.getAbsolutePath();
        File t1fx = new File(absCurPath+"/hzxcf2/nept");
        t1fx.mkdirs();
        File t2fx = new File(absCurPath+"/hzxcf2/nept/d1");
        t2fx.mkdirs();
        File t3fx = new File(absCurPath+"/hzxcf2/nept/d1/sign.txt");
        t3fx.createNewFile();
        pathP = t1fx.getAbsolutePath();
        path  = t3fx.getAbsolutePath();
        resC  = "d1/sign.txt";
        res   = FileUtil.relativizeFilePath(pathP, path);
        assertEquals(new File(resC).getPath(), res);
        t3fx.delete();
        t2fx.delete();
        t1fx.delete();
        t1fx.getParentFile().delete();
    }


    public void testGetFileExtension() {
    	String t1 = "test.ext";
    	String res = FileUtil.getFileExtension(new File(t1));
    	res = new File(res).getName();
    	assertEquals("ext", res);

    	t1 = "test";
    	res = FileUtil.getFileExtension(new File(t1));
    	res = new File(res).getName();
    	assertEquals("", res);

    	t1 = "test.longextensionname";
    	res = FileUtil.getFileExtension(t1);
    	res = new File(res).getName();
    	assertEquals("longextensionname", res);

    	t1 = "test";
    	res = FileUtil.getFileExtension(t1);
    	res = new File(res).getName();
    	assertEquals("", res);

    	t1 = "test.longextensionname.fd";
    	res = FileUtil.getFileExtension(t1);
    	res = new File(res).getName();
    	assertEquals("fd", res);

    	t1 = "test.ext.ext";
    	res = FileUtil.getFileExtension(t1);
    	res = new File(res).getName();
    	assertEquals("ext", res);

    }

    public void testReplaceFileExtension() {
    	String t1 = "test.ext";
    	String res = FileUtil.replaceFileExtension(new File(t1), "res");
    	res = new File(res).getName();
    	assertEquals("test.res", res);

    	t1 = "test";
    	res = FileUtil.replaceFileExtension(new File(t1), "res");
    	res = new File(res).getName();
    	assertEquals("test.res", res);

    	t1 = "test.longextensionname";
    	res = FileUtil.replaceFileExtension(t1, "res");
    	res = new File(res).getName();
    	assertEquals("test.res", res);

    	t1 = "test";
    	res = FileUtil.replaceFileExtension(t1, "res");
    	res = new File(res).getName();
    	assertEquals("test.res", res);

    	t1 = "test.longextensionname.fd";
    	res = FileUtil.replaceFileExtension(t1, "res");
    	res = new File(res).getName();
    	assertEquals("test.longextensionname.res", res);

    	t1 = "test.ext.ext";
    	res = FileUtil.replaceFileExtension(t1, "res");
    	res = new File(res).getName();
    	assertEquals("test.ext.res", res);

    }

}
