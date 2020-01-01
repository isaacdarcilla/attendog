<?php

/**
 *@api {GET}/api/verify Verify
 *@apiDescription Verify QRCode value of scanned student ID
 *@apiName Verify
 *@apiGroup Scanner API
 *@apiVersion 1.0.0
 *
 *@apiParam {string} value The QRCode value from scanned student ID.
 *@apiParam {int} type Access type (ID) selected by teacher.
 *@apiParam {int} section Section (ID) selected by teacher.
 *@apiParam {int} subject Subject (ID) selected by teacher.
 *
 *@apiSuccess (Success Response) {String} status The status handler.
 *<br/> 200 = Success
 *@apiSuccess (Success Response) {String} message The response message.
 *@apiSuccess (Success Response) {String} data The array object of results.
 *
 *@apiSuccessExample Success Response:
 *{
 *  "result": 200,
 *  "message": "Student Verified!",
 *  "data": [
        {
           "name":"Norielle A. Cruz",
           "section":"Abraham",
           "subject":"Science",
           "type":"Time-in"
        }
    ]
 *}
 *
 *@apiError (Fail Response) {String} status The status handler.
 *<br/> 500 = Fail/Invalid/Error
 *@apiError (Fail Response) {String} message The response message.
 *@apiError (Fail Response) {String} data The array object of results.
 *
 *@apiErrorExample Fail Response:
 *{
 *  "result": 500,
 *  "message": "Student is not from this section!",
 *  "data": false
 *}
 *
 */

/**
 *@api {GET}/api/types Access Types
 *@apiDescription List of access types available
 *@apiName Access Types
 *@apiGroup Scanner API
 *@apiVersion 1.0.0
 *
 *@apiSuccess (Success Response) {String} status The status handler.
 *<br/> 200 = Success
 *@apiSuccess (Success Response) {String} message The response message.
 *@apiSuccess (Success Response) {String} data The array object of results.
 *
 *@apiSuccessExample Success Response:
 *{
 *  "result": 200,
 *  "message": "Access Types",
 *  "data": [
        {
           "id": 1,
           "name": "Time-in"
        },
        {
           "id": 2,
           "name": "Time-out"
        }
    ]
 *}
 *
 *@apiError (Fail Response) {String} status The status handler.
 *<br/> 500 = Fail/Invalid/Error
 *@apiError (Fail Response) {String} message The response message.
 *@apiError (Fail Response) {String} data The array object of results.
 *
 *@apiErrorExample Fail Response:
 *{
 *  "result": 500,
 *  "message": "Invalid Request!",
 *  "data": false
 *}
 *
 */

/**
 *@api {GET}/api/sections Sections
 *@apiDescription List of added sections
 *@apiName Sections
 *@apiGroup Scanner API
 *@apiVersion 1.0.0
 *
 *@apiSuccess (Success Response) {String} status The status handler.
 *<br/> 200 = Success
 *@apiSuccess (Success Response) {String} message The response message.
 *@apiSuccess (Success Response) {String} data The array object of results.
 *
 *@apiSuccessExample Success Response:
 *{
 *  "result": 200,
 *  "message": "Sections",
 *  "data": [
        {
           "id": 1,
           "name":"Mahaba"
        },
        {
           "id": 2,
           "name: "Magalang"
        }
    ]
 *}
 *
 *@apiError (Fail Response) {String} status The status handler.
 *<br/> 500 = Fail/Invalid/Error
 *@apiError (Fail Response) {String} message The response message.
 *@apiError (Fail Response) {String} data The array object of results.
 *
 *@apiErrorExample Fail Response:
 *{
 *  "result": 500,
 *  "message": "Invalid Request!",
 *  "data": false
 *}
 *
 */

/**
 *@api {GET}/api/subjects Subjects
 *@apiDescription List of added subjects
 *@apiName Subjects
 *@apiGroup Scanner API
 *@apiVersion 1.0.0
 *
 *@apiSuccess (Success Response) {String} status The status handler.
 *<br/> 200 = Success
 *@apiSuccess (Success Response) {String} message The response message.
 *@apiSuccess (Success Response) {String} data The array object of results.
 *
 *@apiSuccessExample Success Response:
 *{
 *  "result": 200,
 *  "message": "Sections",
 *  "data": [
        {
           "id": 1,
           "name": "Science"
        },
        {
           "id": 2,
           "name": "TLE"
        }
    ]
 *}
 *
 *@apiError (Fail Response) {String} status The status handler.
 *<br/> 500 = Fail/Invalid/Error
 *@apiError (Fail Response) {String} message The response message.
 *@apiError (Fail Response) {String} data The array object of results.
 *
 *@apiErrorExample Fail Response:
 *{
 *  "result": 500,
 *  "message": "Invalid Request!",
 *  "data": false
 *}
 *
 */
