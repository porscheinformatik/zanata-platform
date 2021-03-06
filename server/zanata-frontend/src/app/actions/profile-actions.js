import { createAction } from 'redux-actions'
import { CALL_API } from 'redux-api-middleware'
import { isEmpty, includes, forEach } from 'lodash'
import utilsDate from '../utils/DateHelper'

import {
  getJsonHeaders,
  buildAPIRequest,
  LOAD_USER_REQUEST,
  LOAD_USER_SUCCESS,
  LOAD_USER_FAILURE,
  // eslint-disable-next-line
  APITypes
} from './common-actions'
import { apiUrl, isLoggedIn, links, serverUrl } from '../config'

export const FILTER_UPDATE = 'FILTER_UPDATE'
export const DATE_RANGE_UPDATE = 'DATE_RANGE_UPDATE'
export const SELECT_DAY_UPDATE = 'SELECT_DAY_UPDATE'

export const USER_STATS_REQUEST = 'USER_STATS_REQUEST'
export const USER_STATS_SUCCESS = 'USER_STATS_SUCCESS'
export const USER_STATS_FAILURE = 'USER_STATS_FAILURE'

export const GET_LOCALE_REQUEST = 'GET_LOCALE_REQUEST'
export const GET_LOCALE_SUCCESS = 'GET_LOCALE_SUCCESS'
export const GET_LOCALE_FAILURE = 'GET_LOCALE_FAILURE'

export const updateDateRange = createAction(DATE_RANGE_UPDATE)
export const updateFilter = createAction(FILTER_UPDATE)
export const updateSelectDay = createAction(SELECT_DAY_UPDATE)

// @ts-ignore any
const getStatsEndPoint = (username, fromDate, toDate) => {
  return apiUrl + '/stats/user/' + username + '/' + fromDate + '..' + toDate
}

// @ts-ignore any
const getUserStatistics = (username, fromDate, toDate) => {
  const endpoint = getStatsEndPoint(username, fromDate, toDate)
  /** @type {APITypes} */
  const apiTypes = [
    USER_STATS_REQUEST,
    {
      type: USER_STATS_SUCCESS,
      // @ts-ignore any
      payload: (_action, _state, res) => {
        const contentType = res.headers.get('Content-Type')
        if (contentType && includes(contentType, 'json')) {
          // @ts-ignore any
          return res.json().then((json) => {
            return json
          })
        }
      },
      meta: {
        receivedAt: Date.now()
      }
    },
    USER_STATS_FAILURE
  ]
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'GET', getJsonHeaders(), apiTypes)
  }
}

// @ts-ignore any
const loadUserStats = (username, dateRangeOption) => {
  // @ts-ignore any
  return (dispatch, _getState) => {
    const dateRange = utilsDate.getDateRangeFromOption(dateRangeOption)
    dispatch(getUserStatistics(username, dateRange.fromDate, dateRange.toDate))
  }
}

// @ts-ignore any
const getLocaleDetail = (localeId) => {
  const endpoint = apiUrl + '/locales/locale/' + localeId

  /** @type {APITypes} */
  const apiTypes = [
    GET_LOCALE_REQUEST,
    {
      type: GET_LOCALE_SUCCESS,
      // @ts-ignore any
      payload: (_action, _state, res) => {
        const contentType = res.headers.get('Content-Type')
        if (contentType && includes(contentType, 'json')) {
          // @ts-ignore any
          return res.json().then((json) => {
            return json
          })
        }
      },
      meta: {
        receivedAt: Date.now()
      }
    },
    GET_LOCALE_FAILURE
  ]
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'GET', getJsonHeaders(), apiTypes)
  }
}

// @ts-ignore any
const getUserInfo = (dispatch, username, dateRangeOption) => {
  const endpoint = apiUrl + '/user' + (isEmpty(username) ? '' : '/' + username)

  /** @type {APITypes} */
  const apiTypes = [
    LOAD_USER_REQUEST,
    {
      type: LOAD_USER_SUCCESS,
      // @ts-ignore any
      payload: (_action, _state, res) => {
        const contentType = res.headers.get('Content-Type')
        if (contentType && includes(contentType, 'json')) {
          // @ts-ignore any
          return res.json().then((json) => {
            forEach(json.languageTeams, function (localeId) {
              dispatch(getLocaleDetail(localeId))
            })
            dispatch(loadUserStats(username, dateRangeOption))
            return json
          })
        }
      },
      meta: {
        receivedAt: Date.now()
      }
    },
    LOAD_USER_FAILURE
  ]
  return {
    [CALL_API]: buildAPIRequest(endpoint, 'GET', getJsonHeaders(), apiTypes)
  }
}

// @ts-ignore any
export const profileInitialLoad = (username) => {
  // @ts-ignore any
  return (dispatch, getState) => {
    if (isEmpty(username) && !isLoggedIn) {
      // redirect to login screen if no username is found url
      // and user is not logged in
      window.location.href = serverUrl + links.loginUrl
    } else {
      dispatch(getUserInfo(dispatch, username,
        getState().profile.dateRange))
    }
  }
}

// @ts-ignore any
export const dateRangeChanged = (dataRangeOption) => {
  // @ts-ignore any
  return (dispatch, getState) => {
    const username = getState().profile.user.username
    // @ts-ignore
    dispatch(updateDateRange(dataRangeOption))
    dispatch(loadUserStats(username, dataRangeOption))
  }
}

// @ts-ignore any
export const filterUpdate = (contentState) => {
  // @ts-ignore any
  return (dispatch, getState) => {
    if (getState().profile.contentStateOption !== contentState) {
      // @ts-ignore
      dispatch(updateFilter(contentState))
    }
  }
}

// @ts-ignore any
export const selectDayChanged = (day) => {
  // @ts-ignore any
  return (dispatch, getState) => {
    // click the same day again will cancel selection
    const selectedDay = getState().profile.selectedDay !== day ? day : null
    // @ts-ignore
    dispatch(updateSelectDay(selectedDay))
  }
}
