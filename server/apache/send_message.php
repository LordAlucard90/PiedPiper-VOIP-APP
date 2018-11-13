<?php 
$src = $_POST['src_num'];
$dst = $_POST['dst_num'];
$date = $_POST['datetime'];
$msg = $_POST['msg'];

$content = array();
if(file_exists("$dst.txt")){
    $tmp = file_get_contents("$dst.txt");
    $content = json_decode($tmp);
}

if(isset($_FILES['audio'])){
    $dir = "audios";
    $name = 0;
    while (file_exists("$dir/$name.3gp")) { $name++; }
    if(move_uploaded_file($_FILES["audio"]["tmp_name"], "$dir/$name.3gp")){
        $content[] = array( 'src' => $src, 
                    'datetime' => $date,
                    'audio' => "$name.3gp");
    }
} else {
    $content[] = array( 'src' => $src, 
                'datetime' => $date,
                'msg' => $msg);
}

file_put_contents("$dst.txt", json_encode($content));

?>

