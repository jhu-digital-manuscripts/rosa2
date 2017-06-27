# Create archive with just hamlet
# 
# mkdir archive
# ln -s /mnt/archive/hamletcollection/ archive/

mvn clean install -Diiif.pres.host=rosetest.library.jhu.edu -Diiif.pres.prefix=/iiif-pres-ham -Darchive.path=archive -Diiif.image.host=jdm.library.jhu.edu -Diiif.image.prefix=/iiif-image -Dfsi.url=http://image.library.jhu.edu/fsi/
