/* global jest describe it expect */

import React from 'react'
import * as ReactDOMServer from 'react-dom/server'
import LockIcon from '.'
import Icon from "antd/lib/icon";
import "antd/lib/icon/style/css";

describe('LockIcon', () => {
  it('renders a LockIcon when given a READONLY status', () => {
    const actual = ReactDOMServer.renderToStaticMarkup(
      <LockIcon status={'READONLY'} />
    )
    const expected = ReactDOMServer.renderToStaticMarkup(
      <Icon type="lock" className="s0 txt-warn" />
    )
    expect(actual).toEqual(expected)
  })
  it('renders an empty span when given an ACTIVE status', () => {
    const actual = ReactDOMServer.renderToStaticMarkup(
      <LockIcon status={'ACTIVE'} />
    )
    expect(actual).toEqual('')
  })
})