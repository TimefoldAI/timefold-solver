from jpyinterpreter import is_python_version_supported


def test_version_check():
    assert is_python_version_supported((2, 7, 0, 'Final', 0)) is False
    assert is_python_version_supported((3, 8, 0, 'Final', 0)) is False
    assert is_python_version_supported((3, 9, 0, 'Final', 0)) is False
    assert is_python_version_supported((3, 9, 1, 'Final', 0)) is False
    assert is_python_version_supported((3, 10, 0, 'Final', 0)) is True
    assert is_python_version_supported((3, 10, 5, 'Final', 0)) is True
    assert is_python_version_supported((3, 11, 0, 'Final', 0)) is True
    assert is_python_version_supported((3, 11, 3, 'Final', 0)) is True
    assert is_python_version_supported((3, 12, 0, 'Final', 0)) is True
    assert is_python_version_supported((3, 12, 3, 'Final', 0)) is True
    assert is_python_version_supported((3, 12, 0, 'Final', 0)) is True
    assert is_python_version_supported((3, 13, 0, 'Final', 0)) is False
