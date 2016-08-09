### Status
[![Build Status](https://travis-ci.org/chad-autry/rototone.svg?branch=master)](https://travis-ci.org/chad-autry/rototone)

## rototone
Create playlists of Ringtones and Notification tones for your Android phone.

## Features
* Playlists can be random or sequential
* Playlists can be assigned to individual contacts
* Playlists can be used by default

## Installation on Phone
Rototone is not published to an app store, instead
* Navigate to the [Releases](https://github.com/chad-autry/rototone/releases) page
* Ensure your phone is at least the version supported
* Download the apk on your phone
* Allow installation of Unknown Sources under your security settings
* Install

## Explanation of Required Privileges
* Need to detect when the phone is ringing to rotate tones
* Need to read SMS messages to rotate tones
* Need to read and write contacts to attach tones to them
* WRITE_SETTINGS I'm not actually sure why this is there. Might be able to remove this one
* Need to write external storage to create the rotateable tone files

## Roadmap
* Refactor and cleanup
* Create SMS notification instead of just playing sound
* Add customizeable icon list to SMS notification
