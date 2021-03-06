/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
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
package org.zanata.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import javax.enterprise.inject.Model;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.zanata.async.AsyncTaskHandle;
import org.zanata.async.AsyncTaskHandleManager;
import org.zanata.dao.VersionGroupDAO;
import org.apache.deltaspike.jpa.api.transaction.Transactional;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HProjectLocaleMember;
import org.zanata.model.HProjectMember;
import org.zanata.seam.security.IdentityManager;
import org.zanata.security.annotations.Authenticated;
import org.zanata.security.annotations.CheckLoggedIn;
import org.zanata.dao.AccountDAO;
import org.zanata.dao.ProjectDAO;
import org.zanata.i18n.Messages;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.ActivityService;
import org.zanata.service.GravatarService;
import org.zanata.ui.AbstractListFilter;
import org.zanata.service.LanguageTeamService;
import org.zanata.util.DateUtil;
import javax.annotation.Nullable;

@Named("dashboardAction")
@ViewScoped
@CheckLoggedIn
@Model
@Transactional
public class DashboardAction implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private GravatarService gravatarServiceImpl;
    @Inject
    private ActivityService activityServiceImpl;
    @Inject
    private LanguageTeamService languageTeamServiceImpl;
    @Inject
    private AccountDAO accountDAO;
    @Inject
    private ProjectDAO projectDAO;
    @Inject
    private ZanataIdentity identity;
    @Inject
    private Messages msgs;
    @Inject
    @Authenticated
    private HAccount authenticatedAccount;
    @Inject
    private ProjectFilter projectList;
    @Inject
    private GroupFilter groupList;
    @Inject
    private IdentityManager identityManager;
    @Inject
    private AsyncTaskHandleManager asyncTaskHandleManager;

    public String getUserImageUrl() {
        return gravatarServiceImpl
                .getUserImageUrl(GravatarService.USER_IMAGE_SIZE);
    }

    public String getUsername() {
        return authenticatedAccount.getPerson().getAccount().getUsername();
    }

    public String getUserFullName() {
        return authenticatedAccount.getPerson().getName();
    }

    public String getUserLanguageTeams() {
        HAccount account = accountDAO.findById(authenticatedAccount.getId());
        Stream<String> names =
                account.getPerson().getLanguageMemberships().stream()
                        .map(HLocale::retrieveDisplayName);
        return StringUtils.join(names.iterator(), ", ");
    }

    public String getUserRoles() {
        HAccount account = accountDAO.findById(authenticatedAccount.getId());
        List<String> roles =
            identityManager.getGrantedRoles(account.getUsername());
        return StringUtils.join(roles, ", ");
    }

    public String getLastTranslatedTimeLapseMessage(HProject project) {
        Date lastChanged = project.getLastChanged();
        // TODO i18n needed
        return lastChanged == null ? "never"
                : DateUtil.getHowLongAgoDescription(lastChanged);
    }

    public List<String> getUserProjectRoles(HProject project) {
        Set<String> roles = new HashSet<>();
        for (HProjectMember member: project.getMembers()) {
            if (member.getPerson().equals(authenticatedAccount.getPerson())) {
                roles.add(member.getRole().toString());
            }
        }

        for (HProjectLocaleMember member: project.getLocaleMembers()) {
            if (member.getPerson().equals(authenticatedAccount.getPerson())) {
                roles.add(member.getRole().toString());
            }
        }
        return new ArrayList<>(roles);
    }

    public String getShortTime(Date date) {
        return DateUtil.formatShortDate(date);
    }

    public DashboardUserStats getTodayStats() {
        Date now = new Date();
        return activityServiceImpl.getDashboardUserStatistic(
                authenticatedAccount.getPerson().getId(),
                DateUtil.getStartOfDay(now), DateUtil.getEndOfTheDay(now));
    }

    public DashboardUserStats getWeekStats() {
        Date now = new Date();
        return activityServiceImpl.getDashboardUserStatistic(
                authenticatedAccount.getPerson().getId(),
                DateUtil.getStartOfWeek(now), DateUtil.getEndOfTheWeek(now));
    }

    public DashboardUserStats getMonthStats() {
        Date now = new Date();
        return activityServiceImpl.getDashboardUserStatistic(
                authenticatedAccount.getPerson().getId(),
                DateUtil.getStartOfMonth(now), DateUtil.getEndOfTheMonth(now));
    }

    public boolean isUserReviewer() {
        return languageTeamServiceImpl
                .isUserReviewer(authenticatedAccount.getPerson().getId());
    }

    public String getLastTranslatorMessage(HProject project) {
        return "-";
    }

    public boolean canCreateProject() {
        return identity.hasPermission(new HProject(), "insert");
    }

    public boolean canCreateGroup() {
        return identity.hasPermission(new HIterationGroup(), "insert");
    }

    /**
     * Project list filter. Pages its elements directly from the database.
     */
    public static class ProjectFilter extends AbstractListFilter<HProject>
            implements Serializable {

        private static final long serialVersionUID = -2473428615946542483L;
        @Inject
        @Authenticated
        private HAccount authenticatedAccount;
        @Inject
        private ProjectDAO projectDAO;

        @Override
        protected List<HProject> fetchRecords(int start, int max,
                String filter) {
            return projectDAO.getProjectsForMember(
                    authenticatedAccount.getPerson(), filter, start, max);
        }

        @Override
        protected long fetchTotalRecords(String filter) {
            return projectDAO.getProjectsForMemberCount(
                    authenticatedAccount.getPerson(), filter);
        }
    }

    /**
     * Group list filter. Pages its elements directly from the database.
     */
    public static class GroupFilter extends AbstractListFilter<HIterationGroup>
            implements Serializable {

        private static final long serialVersionUID = 5698872972627107893L;
        @Inject
        @Authenticated
        private HAccount authenticatedAccount;
        @Inject
        private VersionGroupDAO versionGroupDAO;

        @Override
        protected List<HIterationGroup> fetchRecords(int start, int max,
                String filter) {
            return versionGroupDAO.getGroupsByMaintainer(
                    authenticatedAccount.getPerson(), filter, start, max);
        }

        @Override
        protected long fetchTotalRecords(String filter) {
            return versionGroupDAO.getMaintainedGroupCount(
                    authenticatedAccount.getPerson(), filter);
        }
    }

    public ProjectFilter getProjectList() {
        return this.projectList;
    }

    public GroupFilter getGroupList() {
        return this.groupList;
    }

    public Collection<AsyncTaskHandle<?>> getUserTasks() {
        return asyncTaskHandleManager.getTasksFor(identity);
    }

    public void cancelTask(AsyncTaskHandle handle) {
        if (handle.canCancel(identity)) {
            handle.cancel(true);
        }
    }
}
