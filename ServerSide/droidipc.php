<?php
define('DEBUG', false);
function echo_debug($str)
{
    if(DEBUG)
    {
        echo '</br>D:'.$str.'</br>';
    }
}

$query_str=$_SERVER['QUERY_STRING'];
echo_debug($query_str);

define('WP_USE_THEMES', false);
require_once( dirname(__FILE__) . '/../wp-load.php' );

$user_id=get_current_user_id();
echo '{"user_id":"'.$user_id.'"';

if($user_id==0)
{
    $user_id=1;
}

$remote_ip=$_SERVER['REMOTE_ADDR'];
date_default_timezone_set("Asia/Shanghai");
$cur_data_time=date("Y-m-d H:i:s");
$table_name = $wpdb->prefix . 'droidipc';

echo_debug('user_id:'.$user_id);

//ipc login,save client ip 
if($query_str=='=ipc-login')
{
	echo_debug('ipc-login|'.$remote_ip);
	
	//table not exist
	if($wpdb->get_var("show tables like $table_name") != $table_name)
	{
		$wpdb->query("create table $table_name (
		user_id mediumint(9) not null,
		last_login datetime default '0000-00-00 00:00:00' not null,
		ipc_addr varchar(15) default '' not null,
		unique (user_id)
		)");
	}
	
	//id not exist insert new
	if($wpdb->get_var("select user_id from $table_name where user_id='$user_id'")!=$user_id)
	{
		$wpdb->query("insert into $table_name (user_id, last_login, ipc_addr) values ('$user_id', '$cur_data_time', '$remote_ip')");
	}
	//id already exist update
	else
	{
		$wpdb->query("update $table_name set last_login = '$cur_data_time', ipc_addr = '$remote_ip' where user_id = '$user_id'");
	}
	
	echo ',"ipc_addr":"'.$wpdb->get_var("select ipc_addr from $table_name where user_id='$user_id'").'"';
}
//read user client ip,if exist ,link ipc
else if($query_str=='=ipc-get-addr')
{
	echo_debug('read client ip');
	
	//id exist
	if($wpdb->get_var("select user_id from $table_name where user_id='$user_id'")==$user_id)
	{
		$last_login=$wpdb->get_var("select last_login from $table_name where user_id='$user_id'");
		echo_debug("cur_data_time:$cur_data_time, last_login:$last_login, time:".time());
		$last_login=strtotime($cur_data_time)-strtotime($last_login);
		
		echo_debug("last_login:".$last_login);
		
		//ip valid
		if($last_login<20)
		{
			$ipc_addr=$wpdb->get_var("select ipc_addr from $table_name where user_id='$user_id'");
			
			echo ',"ipc_addr":"'.$ipc_addr.'"';
		}
		else
		{
			echo ',"error_ipc":"Addr record time out!"';
		}
	}
	else
	{
		echo ',"error_ipc":"User ID isn\'t exist!"';
	}
	
}
else
{
	echo ',"error_query":"Query string error!"';
}

//$wpdb->show_errors();
//$wpdb->print_error();

echo '}';
?>