#rosa-archive-core test resources

'''
resources/ 
  |--- data/ 
  |--- rosedata/ 
'''

The test resources directory has two subdirectories, 'data' and 'rosedata', both of which contains testing data. This
is meant to simulate an archive with two collections, making 'resources/' the archive directory, and 'resources/data/'
 and 'resources/rosedata/' separate collections. Admittedly, the names could be more clear.

The 'resources/rosedata/' directory holds data copied directly from our 'rosecollection' collection in the archive on
the CIS. The 'resources/data/' directory holds different data meant more to test the full range of behaviors of this
archive core code.