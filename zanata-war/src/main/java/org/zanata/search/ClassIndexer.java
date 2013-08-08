/*
 * Copyright 2013, Red Hat, Inc. and individual contributors
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

package org.zanata.search;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Projections;
import org.hibernate.search.FullTextSession;

/**
 * @author Sean Flanigan <a href="mailto:sflaniga@redhat.com">sflaniga@redhat.com</a>
 *
 */
@Slf4j
public class ClassIndexer<T>
{

   private final AbstractIndexingStrategy<T> indexingStrategy;
   private FullTextSession session;
   private IndexerProcessHandle handle;
   private Class<?> entityType;

   public ClassIndexer(FullTextSession session, IndexerProcessHandle handle,
         Class<?> entityType, AbstractIndexingStrategy<T> indexingStrategy)
   {
      this.session = session;
      this.handle = handle;
      this.entityType = entityType;
      this.indexingStrategy = indexingStrategy;
   }

   public AbstractIndexingStrategy<T> getIndexingStrategy()
   {
      return indexingStrategy;
   }

   public int getEntityCount()
   {
      Long result = (Long) session.createCriteria(entityType).setProjection(Projections.rowCount()).list().get(0);
      return result.intValue();
   }

   public void index()
   {
      log.info("Setting manual-flush and ignore-cache for {}", entityType);
      session.setFlushMode(FlushMode.MANUAL);
      session.setCacheMode(CacheMode.IGNORE);
      try
      {
         indexingStrategy.invoke(handle);
         session.flushToIndexes(); // apply changes to indexes
         session.clear(); // clear since the queue is processed
      }
      catch (Exception e)
      {
         log.warn("Unable to index objects of type {}", e, entityType.getName());
         handle.setHasError(true);
      }
   }

}
