#rosa-archive-core test resources

'''
resources/ 
  |--- archive/
    |--- valid/
      |--- FolgersHa2/
      |--- LudwigXV7/
'''

The initial 'archive' directory represents an archive filled with test data. This archive
contains one test collection, called "valid" which has all completely valid data. The
collection holds two books, FolgersHa2 and LudwigXV7. LudwigXV7 is data from 'rosecollection'
on the CIS. FolgersHa2 contains AoR transcription data.

The test data here should be treated as a read-only collection. Any tests should not write
to this test archive.
