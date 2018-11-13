<?php 
$user = $_POST['number'];
$response = json_encode(array());
if(file_exists("$user.txt")){
    rename("$user.txt", "_$user.txt");
    $response = file_get_contents("_$user.txt");
    unlink("_$user.txt");
}

echo "$response";

?>

