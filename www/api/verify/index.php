<?php

$data = $_GET;

if(isset($data["value"], $data["type"], $data["section"], $data["subject"])){
 die(json_encode([
  "status" => 200,
  "message" => "Student Verified!",
  "data" => [
    "name" => "Norielle Cruz",
    "section" => "Malinaw",
    "subject" => "Math",
    "type" => "Time-in"
  ]
 ]));
} else {
 die(json_encode([
  "status" => 500,
  "message" => "Invalid Request!",
  "data" => false
 ]));
}