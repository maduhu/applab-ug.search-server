<?php
/*
 * MobileSuv - Mobile Surveys Platform
 *
 * Copyright (C) 2006-2010
 * Yo! Uganda Limited and The Grameen Foundation
 * 	
 * All Rights Reserved
 *
 * Unauthorized redistribution of this software in any form or on any
 * medium is strictly prohibited. This software is released under a
 * license agreement and may be used or copied only in accordance with
 * the terms thereof. It is against the law to copy the software on
 * any other medium, except as specifically provided in the license
 * agreement.  No part of this software may be reproduced, stored
 * in a retrieval system, or transmitted in any form or by any means,
 * electronic, mechanical, photocopied, recorded or otherwise,
 * outside the terms of the said license agreement without the prior
 * written permission of Yo! Uganda Limited.
 *
 * YOGBLICCOD331920192_20090909
 */
?>
<?
include("constants.php");
include("functions.php");
include("sessions.php");

dbconnect();
validate_session(); 
check_admin_user();

extract($_GET);

if(!preg_match("/^[0-9]+$/", $categoryId)) { 
   print 'Error';
   exit;
}
//$keyword  = get_keyword_from_id($keywordId);
 $sql = 'SELECT name FROM category where id='.$categoryId;
                $result = mysql_query($sql) or die(mysql_error());
                $record = mysql_fetch_assoc($result);
                $name = $record['name'];

$filename = $name.'-hits.csv';
$filepath = '/tmp/'.$filename;
if(!($fh = fopen($filepath, 'w'))) {
   print 'Can not open file: '.$filepath;
   exit;
}

	$title = "Hits On Category: $name";
	fwrite($fh, "\n$title\n\n");
	fwrite($fh, "Keyword,Hit No.,Phone,Request Message,Reply Message,Time\n");

$sql = "SELECT keyword FROM keyword WHERE categoryId=$categoryId ORDER BY keyword ASC";
   
   if(!($result=mysql_query($sql))) {
      die(mysql_error());
   }
  while($keyword = mysql_fetch_assoc($result)){

        $keywording = str_replace("_"," ",$keyword[keyword]);
        $sql = "SELECT hit.*, DATE_FORMAT(time, '%d/%m/%Y %r') AS hitTime FROM hit WHERE keyword='$keyword[keyword]' OR keyword='$keywording' ORDER BY time DESC";
	if(!($result1=mysql_query($sql))) {
	   print 'Error: '.mysql_error();
	   exit;
	}
	$i = 1;
	while($row=mysql_fetch_assoc($result1)) {
	   fwrite($fh, "\"$keyword[keyword]\",$i.,=\"".get_phone_display_label($row['phone'], 100)."\",\"$row[request]\",\"$row[reply]\",$row[hitTime]\n");
	   $i++;
	}
}
fclose($fh);

header("Content-type: text/x-csv");
header("Content-Disposition: attachment; filename=\"$filename\"");
header("Content-Transfer-Encoding: binary");
header("Content-Length: ".filesize($filepath));

if(!readfile($filepath)) {
  print "Error reading file: $filepath";
  exit;
}

?>
