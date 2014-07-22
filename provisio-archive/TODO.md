- Switch to use Java7 so the Path API can be used
- Have a parameter for the filemode and not just being executable

# Filemode

Extend the ArchiveInputStream so that it returns ExtendedArchiveEntry which provides the file mode

We have instructions to make something executable where maybe the filemode is not set on the source but you want to make sure it's set when you create the archive.
 vs
You just look at the filemode and set the filemode accordingly in the archive.

There maybe cases where you need to manually set the mode and looking at the filemode is never enough
