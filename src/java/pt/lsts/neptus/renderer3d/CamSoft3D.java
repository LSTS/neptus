/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.renderer3d;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

/**
 * @author RJPG
 *
 * this class simulate a camera looking to (0,0,0) 
 * and its use to convert Vector3d -> Vector2d 
 * based on spherical coordinates and roll of viewpoint.
 * (this class doesn't use Java3D or any other 3D engine)   
 */
public class CamSoft3D {
	private double v11, v12, v13, v21, v22, v23, v32, v33, v43;

	public double rho, theta, phi, psi;

	//public Vector3f target;

	public float dist=8f;

	public double pidiv180 = (Math.atan(1) / 45);

	public CamSoft3D() {
		rho = 40;
		theta = Math.PI / 4;
		phi = Math.PI / 4;

		//theta*=pidiv180;
		//phi*=pidiv180;

		coeff();
		dist = 1;
	//	target = new Vector3f(0, 0, 0);
	}

	public void camdef(double rhod, double thetad, double phid, double psid) {
		rho = rhod;
		theta = thetad;
		phi = phid;
		psi = psid;
		//theta*=pidiv180;
		//phi*=pidiv180;
		coeff();
	}

	private void coeff() {
		double costh, sinth, cosph, sinph;
		costh = Math.cos(theta);
		sinth = Math.sin(theta);
		cosph = Math.cos(phi);
		sinph = Math.sin(phi);

		v11 = -sinth;
		v12 = -cosph * costh;
		v13 = -sinph * costh;
		v21 = costh;
		v22 = -cosph * sinth;
		v23 = -sinph * sinth;
		v32 = sinph;
		v33 = -cosph;
		v43 = rho;
	}

	public Vector3f eyecoord(Vector3f pw) {
		Vector3f pe = new Vector3f();
		pe.x = (float) (v11 * pw.x + v21 * pw.y);
		pe.y = (float) (v12 * pw.x + v22 * pw.y + v32 * pw.z);
		pe.z = (float) (v13 * pw.x + v23 * pw.y + v33 * pw.z + v43);

		return pe;
	}

	private Vector2f perspective(Vector3f p) {
		Vector3f pe = null;
		pe = eyecoord(p);
		Vector2f pxy = new Vector2f();
		Vector3f prot = new Vector3f();
		prot.x = (float) ((pe.x * Math.cos(psi)) - (pe.y * Math.sin(psi)));
		prot.y = (float) ((pe.x * Math.sin(psi)) + (pe.y * Math.cos(psi)));
		prot.z = pe.z;
		//prot.x=pe.x;
		//prot.y=pe.y;
		//if(prot.z<0.000001)
		//{
			pxy.x = prot.x/prot.z;
			pxy.y = prot.y/prot.z;
			return pxy;
		//}
		//else
		//{
		//	return  null;
		//}
	}

	public Vector2f to2d(Vector3f p) {
		Vector2f aux = null;
		aux = perspective(p);
		if(aux==null)
			return null;
		Vector2f rt = new Vector2f();
		rt.x = (dist * aux.x);
		rt.y = (-dist * aux.y);
		return rt;
	}
	
	public Vector3f getCamXYZ()
	{
		Vector3f ret=new Vector3f();
		
		ret.x=(float) (rho*Math.sin(phi)*Math.cos(theta));
		ret.y=(float) (rho*Math.sin(phi)*Math.sin(theta));
		ret.z=(float) (rho*Math.cos(phi));
		return ret;
	}
}
