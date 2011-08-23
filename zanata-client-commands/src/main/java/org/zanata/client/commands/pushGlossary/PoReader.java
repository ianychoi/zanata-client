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
package org.zanata.client.commands.pushGlossary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.fedorahosted.tennera.jgettext.Message;
import org.fedorahosted.tennera.jgettext.catalog.parse.MessageStreamParser;
import org.xml.sax.InputSource;
import org.zanata.common.LocaleId;
import org.zanata.rest.dto.Glossary;
import org.zanata.rest.dto.GlossaryEntry;
import org.zanata.rest.dto.GlossaryTerm;

/**
 * 
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 * 
 **/
public class PoReader extends AbstractPushGlossaryReader
{

   @Override
   public Glossary extractGlossary(File glossaryFile) throws IOException
   {
      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(glossaryFile));
      try
      {
         InputSource potInputSource = new InputSource(bis);
         potInputSource.setEncoding("utf8");
         return extractTemplate(potInputSource);
      }
      finally
      {
         bis.close();
      }
   }

   private Glossary extractTemplate(InputSource potInputSource)
   {
      MessageStreamParser messageParser = createParser(potInputSource);

      LocaleId srcLang = new LocaleId(getOpts().getSourceLang());
      LocaleId targetLang = new LocaleId(getOpts().getTransLang());

      Glossary glossary = new Glossary();
      GlossaryEntry entry = new GlossaryEntry();
      entry.setSrcLang(srcLang);

      while (messageParser.hasNext())
      {
         Message message = messageParser.next();

         if (message.isHeader() || message.isObsolete() || message.isPlural())
         {
            // TODO skip for now
         }
         else
         {
            GlossaryTerm srcTerm = new GlossaryTerm();
            srcTerm.setLocale(srcLang);
            srcTerm.setSourcereference(StringUtils.join(message.getSourceReferences(), null));
            srcTerm.setContent(message.getMsgid());
            for (String comment : message.getExtractedComments())
            {
               srcTerm.getComments().add(comment);
            }

            GlossaryTerm targetTerm = new GlossaryTerm();
            targetTerm.setLocale(targetLang);
            targetTerm.setContent(message.getMsgstr());
            for (String comment : message.getComments())
            {
               targetTerm.getComments().add(comment);
            }

            entry.getGlossaryTerms().add(srcTerm);
            entry.getGlossaryTerms().add(targetTerm);
         }
      }

      glossary.getGlossaryEntries().add(entry);

      return glossary;
   }

   static MessageStreamParser createParser(InputSource inputSource)
   {
      MessageStreamParser messageParser;
      if (inputSource.getCharacterStream() != null)
         messageParser = new MessageStreamParser(inputSource.getCharacterStream());
      else if (inputSource.getByteStream() != null)
      {
         if (inputSource.getEncoding() != null)
            messageParser = new MessageStreamParser(inputSource.getByteStream(), Charset.forName(inputSource.getEncoding()));
         else
            messageParser = new MessageStreamParser(inputSource.getByteStream(), Charset.forName("UTF-8"));
      }
      else if (inputSource.getSystemId() != null)
      {
         try
         {
            URL url = new URL(inputSource.getSystemId());

            if (inputSource.getEncoding() != null)
               messageParser = new MessageStreamParser(url.openStream(), Charset.forName(inputSource.getEncoding()));
            else
               messageParser = new MessageStreamParser(url.openStream(), Charset.forName("UTF-8"));
         }
         catch (IOException e)
         {
            // TODO throw stronger typed exception
            throw new RuntimeException("failed to get input from url in inputSource", e);
         }
      }
      else
         // TODO throw stronger typed exception
         throw new RuntimeException("not a valid inputSource");

      return messageParser;
   }
}