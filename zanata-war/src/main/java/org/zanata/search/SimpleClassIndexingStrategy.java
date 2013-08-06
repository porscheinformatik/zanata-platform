/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.search;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.search.FullTextSession;

import lombok.extern.slf4j.Slf4j;

/**
 * Indexing strategy that fetches all instances in a given class and indexes them.
 * This class batches the fetching of the entities and might be a bit slower as it does
 * not account for lazily loaded entity relationships.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Slf4j
public class SimpleClassIndexingStrategy<T> extends AbstractIndexingStrategy<T>
{
   public static final int MAX_QUERY_ROWS = 5000;

   public SimpleClassIndexingStrategy(Class<T> clazz, FullTextSession session)
   {
      super(clazz, session);
   }

   @Override
   protected void onEntityIndexed(int n)
   {
      if (n % MAX_QUERY_ROWS == 0)
      {
         SimpleClassIndexingStrategy.log.info("restarting query for {} (n={})", getClazz(), n);
         getScrollableResults().close();
         setScrollableResults(queryResults(n));
      }
   }

   @Override
   protected ScrollableResults queryResults(int offset)
   {
      Query query = getSession().createQuery("from "+getClazz().getName());
      query.setFirstResult(offset);
      query.setMaxResults(MAX_QUERY_ROWS);
      return query.scroll(ScrollMode.FORWARD_ONLY);
   }

}
