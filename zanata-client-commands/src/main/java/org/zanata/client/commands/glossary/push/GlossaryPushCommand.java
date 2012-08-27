/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.zanata.client.commands.glossary.push;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.adapter.glossary.AbstractGlossaryPushReader;
import org.zanata.adapter.glossary.GlossaryCSVReader;
import org.zanata.adapter.glossary.GlossaryPoReader;
import org.zanata.client.commands.ConfigurableCommand;
import org.zanata.client.commands.OptionsUtil;
import org.zanata.client.config.LocaleMapping;
import org.zanata.common.LocaleId;
import org.zanata.rest.client.ClientUtility;
import org.zanata.rest.client.IGlossaryResource;
import org.zanata.rest.client.ZanataProxyFactory;
import org.zanata.rest.dto.Glossary;

/**
 * 
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 * 
 **/
public class GlossaryPushCommand extends ConfigurableCommand<GlossaryPushOptions>
{
   private static final Logger log = LoggerFactory.getLogger(GlossaryPushCommand.class);

   private static final Map<String, AbstractGlossaryPushReader> glossaryReaders = new HashMap<String, AbstractGlossaryPushReader>();

   private final IGlossaryResource glossaryResource;
   private final URI uri;

   public GlossaryPushCommand(GlossaryPushOptions opts, ZanataProxyFactory factory, IGlossaryResource glossaryResource, URI uri)
   {
      super(opts, factory);
      this.glossaryResource = glossaryResource;
      this.uri = uri;
   }

   private GlossaryPushCommand(GlossaryPushOptions opts, ZanataProxyFactory factory)
   {
      this(opts, factory, factory.getGlossaryResource(), factory.getGlossaryResourceURI());
   }

   public GlossaryPushCommand(GlossaryPushOptions opts)
   {
      this(opts, OptionsUtil.createRequestFactory(opts));

      glossaryReaders.put("po", new GlossaryPoReader(getLocaleFromMap(getOpts().getSourceLang()), getLocaleFromMap(getOpts().getTransLang()), getOpts().getBatchSize(), getOpts().getTreatSourceCommentsAsTarget()));
      glossaryReaders.put("csv", new GlossaryCSVReader(getOpts().getBatchSize(), getOpts().getCommentCols()));
   }

   private LocaleId getLocaleFromMap(String localLocale)
   {
      if (getOpts() != null && getOpts().getLocaleMapList() != null && !getOpts().getLocaleMapList().isEmpty())
      {
         for (LocaleMapping loc : getOpts().getLocaleMapList())
         {
            if (loc.getLocalLocale().equals(localLocale))
            {
               return new LocaleId(loc.getLocale());
            }
         }
      }
      return new LocaleId(localLocale);
   }

   private AbstractGlossaryPushReader getReader(String fileExtension)
   {
      AbstractGlossaryPushReader reader = glossaryReaders.get(fileExtension);
      if (reader == null)
      {
         throw new RuntimeException("unknown file type: " + fileExtension);
      }
      return reader;
   }

   private String validateFileExtensionWithTransLang() throws RuntimeException
   {
      String fileExtension = FilenameUtils.getExtension(getOpts().getGlossaryFile().getName());

      if (StringUtils.isEmpty(getOpts().getTransLang()))
      {
         if (fileExtension.equals("po"))
         {
            throw new RuntimeException("Option 'zanata.transLang' is required for this file type.");
         }
      }
      return fileExtension;
   }

   @Override
   public void run() throws Exception
   {
      log.info("Server: {}", getOpts().getUrl());
      log.info("Username: {}", getOpts().getUsername());
      log.info("Source language: {}", getOpts().getSourceLang());
      log.info("Translation language: {}", getOpts().getTransLang());
      log.info("All translation comment: {}", getOpts().getTreatSourceCommentsAsTarget());
      log.info("Glossary file: {}", getOpts().getGlossaryFile());
      log.info("Batch size: {}", getOpts().getBatchSize());

      File glossaryFile = getOpts().getGlossaryFile();

      if (!glossaryFile.exists())
      {
         throw new RuntimeException("File '" + glossaryFile + "' does not exist - check glossaryFile option");
      }

      if (getOpts().getBatchSize() <= 0)
      {
         throw new RuntimeException("Batch size needs to be 1 or more.");
      }

      String fileExtension = validateFileExtensionWithTransLang();

      AbstractGlossaryPushReader reader = getReader(fileExtension);

      JAXBContext jc = null;
      Marshaller m = null;

      if (getOpts().isDebugSet())
      {
         jc = JAXBContext.newInstance(Glossary.class);
      }
      if (getOpts().isDebugSet())
      {
         m = jc.createMarshaller();
         m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      }

      log.info("pushing glossary document [{}] to server", glossaryFile.getName());

      List<Glossary> glossaries = reader.extractGlossary(glossaryFile);

      int totalEntries = 0;
      for (Glossary glossary : glossaries)
      {
         totalEntries = totalEntries + glossary.getGlossaryEntries().size();
         log.debug("total entries:" + totalEntries);
      }

      int totalDone = 0;
      for (Glossary glossary : glossaries)
      {
         log.debug(glossary.toString());
         ClientResponse<String> response = glossaryResource.put(glossary);
         ClientUtility.checkResult(response, uri);
         totalDone = totalDone + glossary.getGlossaryEntries().size();
         log.info("Pushed " + totalDone + " of " + totalEntries + " entries");
      }
   }
}
