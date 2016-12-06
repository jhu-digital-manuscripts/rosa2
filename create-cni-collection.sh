#! /bin/bash

if [ -e "archive" ]; then
  echo "Unlinking all book symlinks."
  
  unlink archive/aorcollection/FolgersHa2
  unlink archive/aorcollection/HoughtonSTC11402
  unlink archive/aorcollection/PrincetonPA6452

  unlink archive/pizancollection/Francais12779
  unlink archive/pizancollection/Francais1176
  unlink archive/pizancollection/Francais1178

  unlink archive/rosecollection/Walters143
  unlink archive/rosecollection/Morgan948
  unlink archive/rosecollection/SeldenSupra57
else
  echo "Creating new demo archive."
  mkdir archive archive/aorcollection archive/pizancollection archive/rosecollection

  find /mnt/archive/aorcollection/ -maxdepth 1 -type f -exec cp {} archive/aorcollection \;
  find /mnt/archive/pizancollection/ -maxdepth 1 -type f -exec cp {} archive/pizancollection \;
  find /mnt/archive/rosecollection/ -maxdepth 1 -type f -exec cp {} archive/rosecollection \;

  # Add labels to collection files 'config.properties'
  printf "\nlabel=Archaeology of Reading collection\n" >> archive/aorcollection/config.properties
  printf "\nlabel=Christine de Pizan collection\n" >> archive/pizancollection/config.properties
  printf "\nlabel=Roman de la Rose collection\n" >> archive/rosecollection/config.properties

  ln -s /mnt/archive/aorcollection/FolgersHa2/ archive/aorcollection/FolgersHa2
  ln -s /mnt/archive/aorcollection/HoughtonSTC11402/ archive/aorcollection/HoughtonSTC11402
  ln -s /mnt/archive/aorcollection/PrincetonPA6452/ archive/aorcollection/PrincetonPA6452

  ln -s /mnt/archive/pizancollection/Francais12779/ archive/pizancollection/Francais12779
  ln -s /mnt/archive/pizancollection/Francais1176/ archive/pizancollection/Francais1176
  ln -s /mnt/archive/pizancollection/Francais1178/ archive/pizancollection/Francais1178

  ln -s /mnt/archive/rosecollection/Walters143/ archive/rosecollection/Walters143
  ln -s /mnt/archive/rosecollection/Morgan948/ archive/rosecollection/Morgan948
  ln -s /mnt/archive/rosecollection/SeldenSupra57/ archive/rosecollection/SeldenSupra57
fi



