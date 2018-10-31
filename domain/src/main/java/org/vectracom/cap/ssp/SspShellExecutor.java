/**
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.vectracom.cap.ssp;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.mobicents.ss7.management.console.ShellExecutor;

/**
 * @author amit bhayani
 * 
 */
public class SspShellExecutor implements ShellExecutor {

	private static final Logger logger = Logger.getLogger(SspShellExecutor.class);

	private SspManagement mscManagement;
	private SspPropertiesManagement mscPropertiesManagement = SspPropertiesManagement.getInstance();

	/**
	 * 
	 */
	public SspShellExecutor() {
		// TODO Auto-generated constructor stub
	}

	public SspManagement getMscManagement() {
		return mscManagement;
	}

	public void setMscManagement(SspManagement mscManagement) {
		this.mscManagement = mscManagement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.ShellExecutor#execute(java.lang.
	 * String[])
	 */
	@Override
	public String execute(String[] commands) {

		try {
			if (commands.length < 2) {
				return SspOAMMessages.INVALID_COMMAND;
			}
			String command = commands[1];

			if (command.equals("set")) {
				return this.manageSet(commands);
			} else if (command.equals("get")) {
				return this.manageGet(commands);
			}
			return SspOAMMessages.INVALID_COMMAND;
		} catch (Exception e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(commands)), e);
			return e.getMessage();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.ShellExecutor#handles(java.lang.
	 * String)
	 */
	@Override
	public boolean handles(String command) {
		return "msc".equals(command);
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return SspOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
		if (parName.equals(SspPropertiesManagement.MSC_GT)) {
			mscPropertiesManagement.setMscGt(options[3]);
		} else if (parName.equals(SspPropertiesManagement.GMLC_SSN)) {
			int val = Integer.parseInt(options[3]);
			mscPropertiesManagement.setGmlcSsn(val);
		} else if (parName.equals(SspPropertiesManagement.HLR_SSN)) {
			int val = Integer.parseInt(options[3]);
			mscPropertiesManagement.setHlrSsn(val);
		} else if (parName.equals(SspPropertiesManagement.MSC_SSN)) {
			int val = Integer.parseInt(options[3]);
			mscPropertiesManagement.setMscSsn(val);
		} else if (parName.equals(SspPropertiesManagement.MAX_MAP_VERSION)) {
			int val = Integer.parseInt(options[3]);
			mscPropertiesManagement.setMaxMapVersion(val);
		} else {
			return SspOAMMessages.INVALID_COMMAND;
		}

		return SspOAMMessages.PARAMETER_SUCCESSFULLY_SET;
	}

	private String manageGet(String[] options) throws Exception {
		if (options.length == 3) {
			String parName = options[2].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[2]);
			sb.append(" = ");
			if (parName.equals(SspPropertiesManagement.MSC_GT)) {
				sb.append(mscPropertiesManagement.getMscGt());
			} else if (parName.equals(SspPropertiesManagement.GMLC_SSN)) {
				sb.append(mscPropertiesManagement.getGmlcSsn());
			} else if (parName.equals(SspPropertiesManagement.HLR_SSN)) {
				sb.append(mscPropertiesManagement.getHlrSsn());
			} else if (parName.equals(SspPropertiesManagement.MSC_SSN)) {
				sb.append(mscPropertiesManagement.getMscSsn());
			} else if (parName.equals(SspPropertiesManagement.MAX_MAP_VERSION)) {
				sb.append(mscPropertiesManagement.getMaxMapVersion());
			} else {
				return SspOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(SspPropertiesManagement.MSC_GT+" = ");
			sb.append(mscPropertiesManagement.getMscGt());
			sb.append("\n");

			sb.append(SspPropertiesManagement.GMLC_SSN+ " = ");
			sb.append(mscPropertiesManagement.getGmlcSsn());
			sb.append("\n");

			sb.append(SspPropertiesManagement.HLR_SSN+" = ");
			sb.append(mscPropertiesManagement.getHlrSsn());
			sb.append("\n");

			sb.append(SspPropertiesManagement.MSC_SSN +" = ");
			sb.append(mscPropertiesManagement.getMscSsn());
			sb.append("\n");

			sb.append(SspPropertiesManagement.MAX_MAP_VERSION+ " = ");
			sb.append(mscPropertiesManagement.getMaxMapVersion());
			sb.append("\n");

			return sb.toString();
		}
	}
}
