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
header("Expires: Tue, 12 Mar 1910 10:45:00 GMT");
header("Cache-Control: no-cache, must-revalidate");
header("Pragma: no-cache");

include("constants.php");
include("functions.php");
include("sessions.php");

dbconnect();
validate_session(); 
check_admin_user();

check_id($districtId, 'districts.php');
$district = get_table_record('district', $districtId);

if(empty($district)) {
    goto('districts.php');
}
if(isset($_POST["cancel"])) {
    goto('subcounties.php?districtId='.$district['id']);
}

if(count($_POST)) {
    $_POST = strip_form_data($_POST);
	extract($_POST);
}  

if(isset($_POST["submit"])) 
{
   if(!preg_match('/^[a-z]{2,50}$/i', $name)) {
       $errors = 'Subcounty name not valid<br/>';
   }
   else{
       $sql = "SELECT * FROM subcounty WHERE districtId='$district[id]' AND name='$name'";
	   $result=execute_query($sql);
	   if(mysql_num_rows($result))
	       $errors .= 'This Subcounty is already added under "'.$district['name'].'" District<br/>';
   }   
   if(!isset($errors)) {
       $sql = "INSERT INTO subcounty(created, districtId, name) VALUES(NOW(), '$district[id]', '$name')";
	   execute_update($sql);
	   logaction("Add subcounty ($name) to $district[name] District");
	   goto('subcounties.php?districtId='.$district['id']);
  }
}
if(isset($errors)) {
  $errors = "<br/>$errors<br/>";
}

/* menu highlight */
$page = 'userphones';

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<title><?= TITLE ?></title>
<link rel="stylesheet" type="text/css" href="styles/style.css" />
</head>

<body>
<table width="790" border="0" align="center" cellpadding="0" cellspacing="0" class="main">
     <!--DWLayoutTable-->
     <tr>
          <td width="790" height="124" valign="top"><? include('top.php') ?></td>
     </tr>
     <tr>
          		<td height="337" valign="top">
				<table width="100%" border="0" cellpadding="0" cellspacing="0" class="border">
          					<!--DWLayoutTable-->
          					<tr>
          								<td height="22" colspan="4" align="center" valign="middle" class="caption">Add Subcounty to <?= $district['name'] ?> District </td>
                    						</tr>
          					<tr>
          								<td width="50" height="48">&nbsp;</td>
                    						<td colspan="2" valign="top"><? require 'users.settings.php' ?></td>
                    						<td width="71"></td>
               						</tr>
          					
          					
          					
          					<tr>
          								<td height="171">&nbsp;</td>
               									<td width="19">&nbsp;</td>
							                    <td width="648" valign="top">
               														<fieldset>
       																	<legend>Details</legend>
					     						<table width="100%" border="0" cellpadding="0" cellspacing="0">
					     									<!--DWLayoutTable-->
					     									<tr>
					     												<td width="17" height="23">&nbsp;</td>
                   						<td width="196">&nbsp;</td>
                   						<td width="336" valign="top" class="error">
                   									<?= $errors; ?>             </td>
                   						<td width="97">&nbsp;</td>
                  						</tr>
					     									<form method="post">
					     												<tr>
					     															<td height="30">&nbsp;</td>
                    						<td align="right" valign="middle">District:&nbsp;&nbsp;</td>
                    						<td valign="middle"><input name="_name" type="text" class="input" id="_name" value="<?= $district['name'] ?>" size="40" readonly="true" /></td>
                    						<td>&nbsp;</td>
                   						</tr>
					     												<tr>
					     															<td height="30">&nbsp;</td>
                    						<td align="right" valign="middle">
                    									Subcounty:&nbsp;&nbsp;</td>
                    						<td valign="middle"><input name="name" type="text" class="input" id="name" value="<?= $name ?>" size="40" /></td>
                    						<td>&nbsp;</td>
                   						</tr>
					     												<tr>
					     															<td height="35">&nbsp;</td>
                    						<td>&nbsp;</td>
                    						<td valign="middle"><input name="submit" type="submit" class="button" id="submit" value="Add Subcounty" />
                    											<input name="cancel" type="submit" class="button" id="cancel" value="Cancel" /></td>
                    						<td>&nbsp;</td>
                   						</tr>
					     												<tr>
					     												  <td height="38">&nbsp;</td>
					     												  <td>&nbsp;</td>
					     												  <td>&nbsp;</td>
					     												  <td>&nbsp;</td>
		     												  </tr>
					               						</form>
					          						</table>
               									        </fieldset></td>
       						  <td>&nbsp;</td>
				  </tr>
          					<tr>
          					  <td height="94">&nbsp;</td>
          					  <td>&nbsp;</td>
          					  <td>&nbsp;</td>
          					  <td>&nbsp;</td>
				  </tr>
          					
          					
          					
       					</table></td>
     </tr>
      <tr>
          <td height="30" valign="top"><? include('bottom.php') ?></td>
     </tr>
</table>
</body>
</html>
