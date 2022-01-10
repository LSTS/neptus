/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: ineeve
 * July 15, 2019
 */

package pt.lsts.ripples;

public class RipplesAsset {
	private String name;
	private int imcid;
	private RipplesAssetState lastState;
	private RipplesPlan plan;

	public RipplesAsset(String name, int imcid, RipplesAssetState lastState, RipplesPlan plan) {
		setName(name);
		setImcid(imcid);
		setLastState(lastState);
		setPlan(plan);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RipplesPlan getPlan() {
		return plan;
	}

	public void setPlan(RipplesPlan plan) {
		this.plan = plan;
	}

	public RipplesAssetState getLastState() {
		return lastState;
	}

	public void setLastState(RipplesAssetState lastState) {
		this.lastState = lastState;
	}

	public int getImcid() {
		return imcid;
	}

	public void setImcid(int imcid) {
		this.imcid = imcid;
	}

}