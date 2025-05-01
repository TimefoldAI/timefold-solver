import glob
import os
import platform
import subprocess
from pathlib import Path
from setuptools import setup
from setuptools.command.build_py import build_py
from shutil import copyfile


class FetchDependencies(build_py):
    """
    A command class that fetch Java Dependencies and
    add them as files within a python package
    """
    def create_stubs(self, project_root, command):
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies',
                        '-Dpython', '-Dquickly', '-Dexec.skip'], cwd=project_root, check=True)
        subprocess.run([str((project_root / command).absolute()), 'dependency:copy-dependencies',
                        '-Dclassifier=javadoc', '-Dpython', '-Dquickly', '-Dexec.skip'], cwd=project_root, check=True)


    def run(self):
        if not self.dry_run:
            project_root = Path(__file__).parent
            # Do a mvn clean install
            # which is configured to add dependency jars to 'target/dependency'
            command = 'mvnw'
            if platform.system() == 'Windows':
                command = 'mvnw.cmd'
            self.create_stubs(project_root, command)
            subprocess.run([str((project_root / command).absolute()), 'clean', 'install',
                            '-Dpython', '-Dquickly', '-Dexec.skip'], cwd=project_root, check=True)
            classpath_jars = []
            # Add the main artifact
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', '*.jar')))
            # Add the main artifact's dependencies
            classpath_jars.extend(glob.glob(os.path.join(project_root, 'target', 'dependency', '*.jar')))
            self.mkpath(os.path.join(self.build_lib, 'jpyinterpreter', 'jars'))

            # Copy classpath jars to jpyinterpreter.jars
            for file in classpath_jars:
                copyfile(file, os.path.join(self.build_lib, 'jpyinterpreter', 'jars', os.path.basename(file)))

            # Make jpyinterpreter.jars a Python module
            fp = open(os.path.join(self.build_lib, 'jpyinterpreter', 'jars', '__init__.py'), 'w')
            fp.close()
        build_py.run(self)

this_directory = Path(__file__).parent

setup(
    name='jpyinterpreter',
    version='0.0.0a0',
    license='Apache License Version 2.0',
    license_file='LICENSE',
    description='A Python bytecode to Java bytecode translator',
    classifiers=[
        'Development Status :: 1 - Planning',
        'Programming Language :: Python :: 3',
        'Topic :: Software Development :: Libraries :: Java Libraries',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent'
    ],
    packages=['jpyinterpreter', 'java-stubs', 'jpype-stubs', 'org-stubs'],
    package_dir={
        'jpyinterpreter': 'src/main/python',
        # Setup tools need a non-empty directory to use as base
        # Since these packages are generated during the build,
        # we use the src/main/resources package, which does
        # not contain any python files and is already included
        # in the build
        'java-stubs': 'src/main/resources',
        'jpype-stubs': 'src/main/resources',
        'org-stubs': 'src/main/resources',
    },
    test_suite='tests',
    python_requires='>=3.10',
    install_requires=[
        'JPype1==1.5.1',  # pinned to 1.5.1 to avoid https://github.com/jpype-project/jpype/issues/1261
    ],
    cmdclass={'build_py': FetchDependencies},
    package_data={
        'jpyinterpreter.jars': ['*.jar'],
    },
)
