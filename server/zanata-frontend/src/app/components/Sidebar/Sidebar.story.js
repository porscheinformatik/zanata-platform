import React from 'react'
import { storiesOf } from '@storybook/react'
import Sidebar from '.'
import AboutPage from '../../containers/ProjectVersion/AboutPage'
import PeoplePage from '../../containers/ProjectVersion/PeoplePage'
import GroupsPage from '../../containers/ProjectVersion/GroupsPage'
import LanguagesPage from '../../containers/ProjectVersion/LanguagesPage'
import DocumentsPage from '../../containers/ProjectVersion/DocumentsPage'

const aboutText = 'This is one rocking project version. This is the best' +
    ' project version ever.'
const url = 'https://www.google.com'
const linkname = 'Our awesome webpage'

storiesOf('Sidebar', module)
    .add('default', () => (
        <>
          <Sidebar />
          <div className='flexTab'>
            <p>This sidebar example has the active tag applied to both the People
              and Languages pages to provide examples of how this design handles
              sidebar links.</p>
            <p>The sidebar nav has been implemented using &nbsp;
              <a href='https://react-bootstrap.github.io/components.html#navs'>
                react bootstrap components</a>.</p>
          </div>
      </>
    ))
    .add('AboutPage', () => (
        <>
          <Sidebar />
          <AboutPage aboutText={aboutText} aboutLink={url} linkName={linkname} />
        </>
    ))
    .add('PeoplePage', () => (
        <>
          <Sidebar />
          <PeoplePage />
        </>
    ))
    .add('GroupsPage', () => (
        <>
          <Sidebar />
          <GroupsPage />
        </>
    ))
    .add('LanguagesPage', () => (
        <>
          <Sidebar />
          <LanguagesPage />
        </>
    ))
    .add('DocumentsPage', () => (
        <>
          <Sidebar />
          <DocumentsPage />
        </>
    ))
