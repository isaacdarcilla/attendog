<?php

die(json_encode([
  "status" => 200,
  "message" => "Subjects",
  "data" => [
    [
      "id" => 1,
      "name" => "Math"
    ],
    [
      "id" => 2,
      "name" => "Science"
    ]
  ]
 ]));
