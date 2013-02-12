(ns global)

(def music-folders ["/mnt/new-disk/music/Flac"]);, "/mnt/new-disk/music/Good Lossy"])
;(def music-folders ["/home/arj/nyt musik"])

(def valid-extensions-a [".flac", ".mp3", ".ogg"])

; movie

(def movie-folders ["/mnt/1500/movies/" "/mnt/1000/movies/"])
;(def movie-folders ["/home/arj/movies"])

(def valid-extensions-m [".mkv", ".mpg", ".avi"])

(def playback-commands {".mkv" "DISPLAY=\":0\" mplayer -ao alsa -afm hwac3,hwdts -font \"Bitstream Vera Sans\" -ass-font-scale 2 -sid 1 -slang en -vo vdpau -vc ffh264vdpau -fs -monitoraspect 16:9 -demuxer lavf -cache 32768" ".mpg" "DISPLAY=\":0\" sudo -u arj mplayer -lirc -sid 1 -channels 2 -font \"Bitstream Vera Sans\" -ass-font-scale 2 -slang en -vo vdpau -fs -monitoraspect 16:9 -demuxer lavf -cache 32768" ".mpeg" "DISPLAY=\":0\" sudo -u arj mplayer -lirc -sid 1 -channels 2 -font \"Bitstream Vera Sans\" -ass-font-scale 2 -slang en -vo vdpau -fs -monitoraspect 16:9 -demuxer lavf -cache 32768" ".avi" "DISPLAY=\":0\" sudo -u arj mplayer -lirc -sid 1 -channels 2 -font \"Bitstream Vera Sans\" -ass-font-scale 2 -slang en -vo vdpau -fs -monitoraspect 16:9 -cache 32768" "dvd" "DISPLAY=\":0\" sudo -u arj mplayer -lirc -sid 1 -channels 2 -font \"Bitstream Vera Sans\" -ass-font-scale 2 -slang en -vo vdpau -dvd-device /dev/sr0 -fs -monitoraspect 16:9 -demuxer lavf -cache 32768"})


(def web-folder "/home/arj/mumocomp/")
