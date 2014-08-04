while(<>) {
    s/[\r\n]//g;
    s/\\/\\\\/g;
    s/\"/\\"/g;
    s/\'/\\'/g;
    if ($started) {
        print "+\"$_\\n\"\n";
    }
    else {
        print "\"$_\\n\"\n";
        $started = 1;
    }
}
print ";\n";        
