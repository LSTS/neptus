/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 26/Fev/2005
 */
package pt.lsts.neptus.junit.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import pt.lsts.neptus.util.FileUtil;

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
