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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
 * @author amit bhayani
 * 
 */
public class SspPropertiesManagement implements SspPropertiesManagementMBean {

	private static final Logger logger = Logger.getLogger(SspPropertiesManagement.class);

	protected static final String MSC_GT = "gmlcgt";
	protected static final String GMLC_SSN = "gmlcssn";
	protected static final String HLR_SSN = "hlrssn";
	protected static final String MSC_SSN = "mscssn";
	protected static final String MAX_MAP_VERSION = "maxmapv";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "sspproperties.xml";

	private static SspPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private String mscGt = "000000";
	private int gmlcSsn = 6;
	private int hlrSsn = 6;
	private int mscSsn = 8;
	private int maxMapVersion = 3;

	private SspPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	protected static SspPropertiesManagement getInstance(String name) {
		if (instance == null) {
			instance = new SspPropertiesManagement(name);
		}
		return instance;
	}

	public static SspPropertiesManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

	@Override
	public String getMscGt() {
		return mscGt;
	}

	@Override
	public void setMscGt(String mscGt) {
		this.mscGt = mscGt;
        this.store();
	}

	@Override
	public int getGmlcSsn() {
		return gmlcSsn;
	}

	@Override
	public void setGmlcSsn(int gmlcSsn) {
		this.gmlcSsn = gmlcSsn;
        this.store();
	}

	@Override
	public int getHlrSsn() {
		return hlrSsn;
	}

	@Override
	public void setHlrSsn(int hlrSsn) {
		this.hlrSsn = hlrSsn;
        this.store();
	}

	@Override
	public int getMscSsn() {
		return mscSsn;
	}

	@Override
	public void setMscSsn(int mscSsn) {
		this.mscSsn = mscSsn;
        this.store();
	}

	@Override
	public int getMaxMapVersion() {
		return maxMapVersion;
	}

	@Override
	public void setMaxMapVersion(int maxMapVersion) {
		this.maxMapVersion = maxMapVersion;
        this.store();
	}

	public void start() throws Exception {

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(SspManagement.GMLC_PERSIST_DIR_KEY,
							System.getProperty(SspManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading SSP Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SSP configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

			writer.write(this.mscGt, MSC_GT, String.class);
			writer.write(this.gmlcSsn, GMLC_SSN, Integer.class);
			writer.write(this.hlrSsn, HLR_SSN, Integer.class);
			writer.write(this.mscSsn, MSC_SSN, Integer.class);
			writer.write(this.maxMapVersion, MAX_MAP_VERSION, Integer.class);
			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the Rule state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			this.mscGt = reader.read(MSC_GT, String.class);
			this.gmlcSsn = reader.read(GMLC_SSN, Integer.class);
			this.hlrSsn = reader.read(HLR_SSN, Integer.class);
			this.mscSsn = reader.read(MSC_SSN, Integer.class);
			this.maxMapVersion = reader.read(MAX_MAP_VERSION, Integer.class);

			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}

}
