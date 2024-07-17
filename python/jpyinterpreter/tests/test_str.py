from .conftest import verifier_for
from typing import Union


########################################
# Sequence methods
########################################

def test_membership():
    def membership(tested: str, x: str) -> bool:
        return x in tested

    def not_membership(tested: str, x: str) -> bool:
        return x not in tested

    membership_verifier = verifier_for(membership)
    not_membership_verifier = verifier_for(not_membership)

    membership_verifier.verify('hello world', 'world', expected_result=True)
    not_membership_verifier.verify('hello world', 'world', expected_result=False)

    membership_verifier.verify('hello world', 'test', expected_result=False)
    not_membership_verifier.verify('hello world', 'test', expected_result=True)

    membership_verifier.verify('hello world', '', expected_result=True)
    not_membership_verifier.verify('hello world', '', expected_result=False)


def test_concat():
    def concat(x: str, y: str) -> tuple:
        out = x + y
        return out, out is x, out is y

    concat_verifier = verifier_for(concat)

    concat_verifier.verify('hello ', 'world', expected_result=('hello world', False, False))
    concat_verifier.verify('', 'hello world', expected_result=('hello world', False, True))
    concat_verifier.verify('hello world', '', expected_result=('hello world', True, False))
    concat_verifier.verify('world ', 'hello', expected_result=('world hello', False, False))


def test_repeat():
    def left_repeat(x: str, y: int) -> tuple:
        out = x * y
        return out, out is x, out is y

    def right_repeat(x: int, y: str) -> tuple:
        out = x * y
        return out, out is x, out is y

    left_repeat_verifier = verifier_for(left_repeat)
    right_repeat_verifier = verifier_for(right_repeat)

    left_repeat_verifier.verify('hi', 1, expected_result=('hi', True, False))
    left_repeat_verifier.verify('abc', 2, expected_result=('abcabc', False, False))
    left_repeat_verifier.verify('a', 4, expected_result=('aaaa', False, False))
    left_repeat_verifier.verify('test', 0, expected_result=('', False, False))
    left_repeat_verifier.verify('test', -1, expected_result=('', False, False))
    left_repeat_verifier.verify('test', -2, expected_result=('', False, False))

    right_repeat_verifier.verify(1, 'hi', expected_result=('hi', False, True))
    right_repeat_verifier.verify(2, 'abc', expected_result=('abcabc', False, False))
    right_repeat_verifier.verify(4, 'a', expected_result=('aaaa', False, False))
    right_repeat_verifier.verify(0, 'test', expected_result=('', False, False))
    right_repeat_verifier.verify(-1, 'test', expected_result=('', False, False))
    right_repeat_verifier.verify(-2, 'test', expected_result=('', False, False))


def test_get_item():
    def get_item(tested: str, index: int) -> str:
        return tested[index]

    get_item_verifier = verifier_for(get_item)

    get_item_verifier.verify('abc', 1, expected_result='b')
    get_item_verifier.verify('abc', -1, expected_result='c')
    get_item_verifier.verify('abcd', -1, expected_result='d')
    get_item_verifier.verify('abcd', -2, expected_result='c')
    get_item_verifier.verify('abcd', 0, expected_result='a')
    get_item_verifier.verify('abc', 3, expected_error=IndexError)
    get_item_verifier.verify('abc', -4, expected_error=IndexError)


def test_get_slice():
    def get_slice(tested: str, start: Union[int, None], end: Union[int, None]) -> str:
        return tested[start:end]

    get_slice_verifier = verifier_for(get_slice)

    get_slice_verifier.verify('abcde', 1, 3, expected_result='bc')
    get_slice_verifier.verify('abcde', -3, -1, expected_result='cd')

    get_slice_verifier.verify('abcde', 0, -2, expected_result='abc')
    get_slice_verifier.verify('abcde', -3, 4, expected_result='cd')

    get_slice_verifier.verify('abcde', 3, 1, expected_result='')
    get_slice_verifier.verify('abcde', -1, -3, expected_result='')

    get_slice_verifier.verify('abcde', 100, 1000, expected_result='')
    get_slice_verifier.verify('abcde', 0, 1000, expected_result='abcde')

    get_slice_verifier.verify('abcde', 1, None, expected_result='bcde')
    get_slice_verifier.verify('abcde', None, 2, expected_result='ab')
    get_slice_verifier.verify('abcde', None, None, expected_result='abcde')


def test_get_slice_with_step():
    def get_slice_with_step(tested: str, start: Union[int, None], end: Union[int, None], step: Union[int, None]) -> str:
        return tested[start:end:step]

    get_slice_verifier = verifier_for(get_slice_with_step)

    get_slice_verifier.verify('abcde', 0, None, 2, expected_result='ace')
    get_slice_verifier.verify('abcde', 1, None, 2, expected_result='bd')
    get_slice_verifier.verify('abcde', 0, 5, 2, expected_result='ace')
    get_slice_verifier.verify('abcde', 1, 5, 2, expected_result='bd')
    get_slice_verifier.verify('abcde', 0, -1, 2, expected_result='ac')
    get_slice_verifier.verify('abcde', 1, -1, 2, expected_result='bd')

    get_slice_verifier.verify('abcde', 4, None, -2, expected_result='eca')
    get_slice_verifier.verify('abcde', 3, None, -2, expected_result='db')
    get_slice_verifier.verify('abcde', -1, -6, -2, expected_result='eca')
    get_slice_verifier.verify('abcde', -2, -6, -2, expected_result='db')
    get_slice_verifier.verify('abcde', 4, 0, -2, expected_result='ec')
    get_slice_verifier.verify('abcde', 3, 0, -2, expected_result='db')

    get_slice_verifier.verify('abcde', 0, None, None, expected_result='abcde')
    get_slice_verifier.verify('abcde', 0, 3, None, expected_result='abc')

    get_slice_verifier.verify('abcde', 3, 1, -1, expected_result='dc')
    get_slice_verifier.verify('abcde', -1, -3, -1, expected_result='ed')
    get_slice_verifier.verify('abcde', 3, 1, 1, expected_result='')
    get_slice_verifier.verify('abcde', -1, -3, 1, expected_result='')


def test_len():
    def length(tested: str) -> int:
        return len(tested)

    len_verifier = verifier_for(length)

    len_verifier.verify('', expected_result=0)
    len_verifier.verify('a', expected_result=1)
    len_verifier.verify('ab', expected_result=2)
    len_verifier.verify('cba', expected_result=3)


def test_index():
    def index(tested: str, item: str) -> int:
        return tested.index(item)

    def index_start(tested: str, item: str, start: int) -> int:
        return tested.index(item, start)

    def index_start_end(tested: str, item: str, start: int, end: int) -> int:
        return tested.index(item, start, end)

    index_verifier = verifier_for(index)
    index_start_verifier = verifier_for(index_start)
    index_start_end_verifier = verifier_for(index_start_end)

    index_verifier.verify('abcabc', 'a', expected_result=0)
    index_verifier.verify('abcabc', 'b', expected_result=1)
    index_verifier.verify('abcabc', 'd', expected_error=ValueError)

    index_start_verifier.verify('abcabc', 'a', 1, expected_result=3)
    index_start_verifier.verify('abcabc', 'a', 5, expected_error=ValueError)
    index_start_verifier.verify('abcabc', 'b', 1, expected_result=1)
    index_start_verifier.verify('abcabc', 'c', 1, expected_result=2)
    index_start_verifier.verify('abcabc', 'd', 1, expected_error=ValueError)

    index_start_verifier.verify('abcabc', 'a', -3, expected_result=3)
    index_start_verifier.verify('abcabc', 'b', -2, expected_result=4)
    index_start_verifier.verify('abcabc', 'c', -2, expected_result=5)
    index_start_verifier.verify('abcabc', 'd', -2, expected_error=ValueError)

    index_start_end_verifier.verify('abcabc', 'a', 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify('abcabc', 'b', 1, 2, expected_result=1)
    index_start_end_verifier.verify('abcabc', 'c', 1, 2, expected_error=ValueError)
    index_start_end_verifier.verify('abcabc', 'd', 1, 2, expected_error=ValueError)

    index_start_end_verifier.verify('abcabc', 'a', -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify('abcabc', 'b', -2, -1, expected_result=4)
    index_start_end_verifier.verify('abcabc', 'c', -2, -1, expected_error=ValueError)
    index_start_end_verifier.verify('abcabc', 'd', -2, -1, expected_error=ValueError)


def test_count():
    def count(tested: str, item: str) -> int:
        return tested.count(item)

    count_verifier = verifier_for(count)

    count_verifier.verify('abc', 'a', expected_result=1)
    count_verifier.verify('abc', 'b', expected_result=1)
    count_verifier.verify('abc', 'c', expected_result=1)
    count_verifier.verify('abc', 'd', expected_result=0)

    count_verifier.verify('abca', 'a', expected_result=2)
    count_verifier.verify('aaca', 'a', expected_result=3)
    count_verifier.verify('', 'a', expected_result=0)


########################################
# String operations
########################################
def test_interpolation():
    def interpolation(tested: str, values: object) -> str:
        return tested % values

    interpolation_verifier = verifier_for(interpolation)

    interpolation_verifier.verify('%d', 100, expected_result='100')
    interpolation_verifier.verify('%d', 0b1111, expected_result='15')
    interpolation_verifier.verify('%s', 'foo', expected_result='foo')
    interpolation_verifier.verify('%s %s', ('foo', 'bar'), expected_result='foo bar')
    interpolation_verifier.verify('%(foo)s', {'foo': 10, 'bar': 20}, expected_result='10')

    interpolation_verifier.verify('%d', 101, expected_result='101')
    interpolation_verifier.verify('%i', 101, expected_result='101')

    interpolation_verifier.verify('%o', 27, expected_result='33')
    interpolation_verifier.verify('%#o', 27, expected_result='0o33')

    interpolation_verifier.verify('%x', 27, expected_result='1b')
    interpolation_verifier.verify('%X', 27, expected_result='1B')
    interpolation_verifier.verify('%#x', 27, expected_result='0x1b')
    interpolation_verifier.verify('%#X', 27, expected_result='0X1B')

    interpolation_verifier.verify('%03d', 1, expected_result='001')
    interpolation_verifier.verify('%-5d', 1, expected_result='1    ')
    interpolation_verifier.verify('%0-5d', 1, expected_result='1    ')

    interpolation_verifier.verify('%d', 1, expected_result='1')
    interpolation_verifier.verify('%d', -1, expected_result='-1')
    interpolation_verifier.verify('% d', 1, expected_result=' 1')
    interpolation_verifier.verify('% d', -1, expected_result='-1')
    interpolation_verifier.verify('%+d', 1, expected_result='+1')
    interpolation_verifier.verify('%+d', -1, expected_result='-1')

    interpolation_verifier.verify('%f', 3.14, expected_result='3.140000')
    interpolation_verifier.verify('%F', 3.14, expected_result='3.140000')
    interpolation_verifier.verify('%.1f', 3.14, expected_result='3.1')
    interpolation_verifier.verify('%.2f', 3.14, expected_result='3.14')
    interpolation_verifier.verify('%.3f', 3.14, expected_result='3.140')

    interpolation_verifier.verify('%g', 1234567890, expected_result='1.23457e+09')
    interpolation_verifier.verify('%G', 1234567890, expected_result='1.23457E+09')
    interpolation_verifier.verify('%e', 1234567890, expected_result='1.234568e+09')
    interpolation_verifier.verify('%E', 1234567890, expected_result='1.234568E+09')

    interpolation_verifier.verify('ABC %c', 10, expected_result='ABC \n')
    interpolation_verifier.verify('ABC %c', 67, expected_result='ABC C')
    interpolation_verifier.verify('ABC %c', 68, expected_result='ABC D')
    interpolation_verifier.verify('ABC %c', 'D', expected_result='ABC D')
    interpolation_verifier.verify('ABC %s', 'test', expected_result='ABC test')
    interpolation_verifier.verify('ABC %r', 'test', expected_result='ABC \'test\'')

    interpolation_verifier.verify('Give it %d%%!', 100, expected_result='Give it 100%!')
    interpolation_verifier.verify('Give it %(all-you-got)d%%!', {'all-you-got': 100}, expected_result='Give it 100%!')


########################################
# String methods
########################################


def test_capitalize():
    def capitalize(tested: str) -> str:
        return tested.capitalize()

    capitalize_verifier = verifier_for(capitalize)

    capitalize_verifier.verify('', expected_result='')
    capitalize_verifier.verify('test', expected_result='Test')
    capitalize_verifier.verify('TEST', expected_result='Test')
    capitalize_verifier.verify('hello world', expected_result='Hello world')
    capitalize_verifier.verify('Hello World', expected_result='Hello world')
    capitalize_verifier.verify('HELLO WORLD', expected_result='Hello world')


def test_casefold():
    def casefold(tested: str) -> str:
        return tested.casefold()

    casefold_verifier = verifier_for(casefold)

    casefold_verifier.verify('', expected_result='')
    casefold_verifier.verify('test', expected_result='test')
    casefold_verifier.verify('TEST', expected_result='test')
    casefold_verifier.verify('hello world', expected_result='hello world')
    casefold_verifier.verify('Hello World', expected_result='hello world')
    casefold_verifier.verify('HELLO WORLD', expected_result='hello world')

    casefold_verifier.verify('ßest', expected_result='ssest')


def test_center():
    def center(tested: str, width: int) -> str:
        return tested.center(width)

    def center_with_fill(tested: str, width: int, fill: str) -> str:
        return tested.center(width, fill)

    center_verifier = verifier_for(center)
    center_with_fill_verifier = verifier_for(center_with_fill)

    center_verifier.verify('test', 10, expected_result='   test   ')
    center_verifier.verify('test', 9, expected_result='   test  ')
    center_verifier.verify('test', 4, expected_result='test')
    center_verifier.verify('test', 2, expected_result='test')

    center_with_fill_verifier.verify('test', 10, '#', expected_result='###test###')
    center_with_fill_verifier.verify('test', 9, '#', expected_result='###test##')
    center_with_fill_verifier.verify('test', 4, '#', expected_result='test')
    center_with_fill_verifier.verify('test', 2, '#', expected_result='test')


def test_count_str():
    def count(tested: str, item: str) -> int:
        return tested.count(item)

    def count_from_start(tested: str, item: str, start: int) -> int:
        return tested.count(item, start)

    def count_between(tested: str, item: str, start: int, end: int) -> int:
        return tested.count(item, start, end)

    count_verifier = verifier_for(count)
    count_from_start_verifier = verifier_for(count_from_start)
    count_between_verifier = verifier_for(count_between)

    count_verifier.verify('abc', 'a', expected_result=1)
    count_verifier.verify('abc', 'b', expected_result=1)
    count_verifier.verify('abc', 'c', expected_result=1)
    count_verifier.verify('abc', 'd', expected_result=0)

    count_verifier.verify('abca', 'a', expected_result=2)
    count_verifier.verify('aaca', 'a', expected_result=3)
    count_verifier.verify('', 'a', expected_result=0)

    count_from_start_verifier.verify('abc', 'a', 1, expected_result=0)
    count_from_start_verifier.verify('abc', 'b', 1, expected_result=1)
    count_from_start_verifier.verify('abc', 'c', 1, expected_result=1)
    count_from_start_verifier.verify('abc', 'd', 1, expected_result=0)

    count_from_start_verifier.verify('abca', 'a', 1, expected_result=1)
    count_from_start_verifier.verify('aaca', 'a', 1, expected_result=2)
    count_from_start_verifier.verify('', 'a', 1, expected_result=0)

    count_between_verifier.verify('abc', 'a', 1, 2, expected_result=0)
    count_between_verifier.verify('abc', 'b', 1, 2, expected_result=1)
    count_between_verifier.verify('abc', 'c', 1, 2, expected_result=0)
    count_between_verifier.verify('abc', 'd', 1, 2, expected_result=0)

    count_between_verifier.verify('abca', 'a', 1, 2, expected_result=0)
    count_between_verifier.verify('abca', 'a', 1, 4, expected_result=1)
    count_between_verifier.verify('abca', 'a', 0, 2, expected_result=1)
    count_between_verifier.verify('aaca', 'a', 1, 2, expected_result=1)
    count_between_verifier.verify('', 'a', 1, 2, expected_result=0)


def test_endswith():
    def endswith(tested: str, suffix: str) -> bool:
        return tested.endswith(suffix)

    def endswith_start(tested: str, suffix: str, start: int) -> bool:
        return tested.endswith(suffix, start)

    def endswith_between(tested: str, suffix: str, start: int, end: int) -> bool:
        return tested.endswith(suffix, start, end)

    endswith_verifier = verifier_for(endswith)
    endswith_start_verifier = verifier_for(endswith_start)
    endswith_between_verifier = verifier_for(endswith_between)

    endswith_verifier.verify('hello world', 'world', expected_result=True)
    endswith_verifier.verify('hello world', 'hello', expected_result=False)
    endswith_verifier.verify('hello', 'hello world', expected_result=False)
    endswith_verifier.verify('hello world', 'hello world', expected_result=True)

    endswith_start_verifier.verify('hello world', 'world', 6, expected_result=True)
    endswith_start_verifier.verify('hello world', 'hello', 6, expected_result=False)
    endswith_start_verifier.verify('hello', 'hello world', 6, expected_result=False)
    endswith_start_verifier.verify('hello world', 'hello world', 6, expected_result=False)

    endswith_between_verifier.verify('hello world', 'world', 6, 11, expected_result=True)
    endswith_between_verifier.verify('hello world', 'world', 7, 11, expected_result=False)
    endswith_between_verifier.verify('hello world', 'hello', 0, 5, expected_result=True)
    endswith_between_verifier.verify('hello', 'hello world', 0, 5, expected_result=False)
    endswith_between_verifier.verify('hello world', 'hello world', 5, 11, expected_result=False)


def test_expandtabs():
    def expandtabs(tested: str) -> str:
        return tested.expandtabs()

    def expandtabs_with_tabsize(tested: str, tabsize: int) -> str:
        return tested.expandtabs(tabsize)

    expandtabs_verifier = verifier_for(expandtabs)
    expandtabs_with_tabsize_verifier = verifier_for(expandtabs_with_tabsize)

    expandtabs_verifier.verify('01\t012\t0123\t01234', expected_result='01      012     0123    01234')
    expandtabs_with_tabsize_verifier.verify('01\t012\t0123\t01234', 8, expected_result='01      012     0123    01234')
    expandtabs_with_tabsize_verifier.verify('01\t012\t0123\t01234', 4, expected_result='01  012 0123    01234')


def test_find():
    def find(tested: str, item: str) -> int:
        return tested.find(item)

    def find_start_verifier(tested: str, item: str, start: int) -> int:
        return tested.find(item, start)

    def find_start_end_verifier(tested: str, item: str, start: int, end: int) -> int:
        return tested.find(item, start, end)

    find_verifier = verifier_for(find)
    find_start_verifier = verifier_for(find_start_verifier)
    find_start_end_verifier = verifier_for(find_start_end_verifier)

    find_verifier.verify('abcabc', 'a', expected_result=0)
    find_verifier.verify('abcabc', 'b', expected_result=1)
    find_verifier.verify('abcabc', 'd', expected_result=-1)

    find_start_verifier.verify('abcabc', 'a', 1, expected_result=3)
    find_start_verifier.verify('abcabc', 'a', 5, expected_result=-1)
    find_start_verifier.verify('abcabc', 'b', 1, expected_result=1)
    find_start_verifier.verify('abcabc', 'c', 1, expected_result=2)
    find_start_verifier.verify('abcabc', 'd', 1, expected_result=-1)

    find_start_verifier.verify('abcabc', 'a', -3, expected_result=3)
    find_start_verifier.verify('abcabc', 'b', -2, expected_result=4)
    find_start_verifier.verify('abcabc', 'c', -2, expected_result=5)
    find_start_verifier.verify('abcabc', 'd', -2, expected_result=-1)

    find_start_end_verifier.verify('abcabc', 'a', 1, 2, expected_result=-1)
    find_start_end_verifier.verify('abcabc', 'b', 1, 2, expected_result=1)
    find_start_end_verifier.verify('abcabc', 'c', 1, 2, expected_result=-1)
    find_start_end_verifier.verify('abcabc', 'd', 1, 2, expected_result=-1)

    find_start_end_verifier.verify('abcabc', 'a', -2, -1, expected_result=-1)
    find_start_end_verifier.verify('abcabc', 'b', -2, -1, expected_result=4)
    find_start_end_verifier.verify('abcabc', 'c', -2, -1, expected_result=-1)
    find_start_end_verifier.verify('abcabc', 'd', -2, -1, expected_result=-1)


def test_format():
    def my_format(tested: str, positional: Union[tuple, None], keywords: Union[dict, None]) -> str:
        if positional is None:
            return tested.format(**keywords)
        elif keywords is None:
            return tested.format(*positional)
        else:
            return tested.format(*positional, **keywords)

    format_verifier = verifier_for(my_format)

    format_verifier.verify('{0}, {1}, {2}', ('a', 'b', 'c'), None, expected_result='a, b, c')
    format_verifier.verify('{}, {}, {}', ('a', 'b', 'c'), None, expected_result='a, b, c')
    format_verifier.verify('{2}, {1}, {0}', ('a', 'b', 'c'), None, expected_result='c, b, a')
    format_verifier.verify('{0}{1}{0}', ('abra', 'cad'), None, expected_result='abracadabra')
    format_verifier.verify('Coordinates: {latitude}, {longitude}', None, {'latitude': '37.24N',
                                                                          'longitude': '-115.81W'},
                           expected_result='Coordinates: 37.24N, -115.81W')
    format_verifier.verify("repr() shows quotes: {!r}; str() doesn't: {!s}", ('test1', 'test2'), None,
                           expected_result="repr() shows quotes: 'test1'; str() doesn't: test2")
    format_verifier.verify('{:<30}', ('left aligned',), None,
                           expected_result='left aligned                  ')
    format_verifier.verify('{:>30}', ('right aligned',), None,
                           expected_result='                 right aligned')
    format_verifier.verify('{:^30}', ('centered',), None,
                           expected_result='           centered           ')
    format_verifier.verify('{:*^30}', ('centered',), None,
                           expected_result='***********centered***********')
    format_verifier.verify('{:+f}; {:+f}', (3.14, -3.14), None,
                           expected_result='+3.140000; -3.140000')
    format_verifier.verify('{: f}; {: f}', (3.14, -3.14), None,
                           expected_result=' 3.140000; -3.140000')
    format_verifier.verify('{:-f}; {:-f}', (3.14, -3.14), None,
                           expected_result='3.140000; -3.140000')
    format_verifier.verify("int: {0:d};  hex: {0:x};  oct: {0:o};  bin: {0:b}", (42,), None,
                           expected_result='int: 42;  hex: 2a;  oct: 52;  bin: 101010')
    format_verifier.verify("int: {0:d};  hex: {0:#x};  oct: {0:#o};  bin: {0:#b}", (42,), None,
                           expected_result='int: 42;  hex: 0x2a;  oct: 0o52;  bin: 0b101010')
    format_verifier.verify("{:,}", (1234567890,), None,
                           expected_result='1,234,567,890')
    format_verifier.verify("{:_}", (1234567890,), None,
                           expected_result='1_234_567_890')
    format_verifier.verify("Correct answers: {:.2%}", (19/22,), None,
                           expected_result='Correct answers: 86.36%')


def test_isalnum():
    def isalnum(tested: str) -> bool:
        return tested.isalnum()

    isalnum_verifier = verifier_for(isalnum)

    isalnum_verifier.verify('', expected_result=False)

    isalnum_verifier.verify('abc', expected_result=True)
    isalnum_verifier.verify('αβγ', expected_result=True)

    isalnum_verifier.verify('ABC', expected_result=True)
    isalnum_verifier.verify('ΑΒΓ', expected_result=True)

    isalnum_verifier.verify('ǋǲᾩ', expected_result=True)

    isalnum_verifier.verify('ᐎᐐᐊ', expected_result=True)

    isalnum_verifier.verify('ʷʰʸ', expected_result=True)

    isalnum_verifier.verify('123', expected_result=True)
    isalnum_verifier.verify('౧౨౩', expected_result=True)

    isalnum_verifier.verify('ⅠⅡⅢ', expected_result=True)
    isalnum_verifier.verify('¼½¾', expected_result=True)

    isalnum_verifier.verify('ABC123', expected_result=True)
    isalnum_verifier.verify('αβγ౧౨౩', expected_result=True)

    isalnum_verifier.verify('+', expected_result=False)
    isalnum_verifier.verify('±', expected_result=False)
    isalnum_verifier.verify('×', expected_result=False)

    isalnum_verifier.verify('©', expected_result=False)

    isalnum_verifier.verify('[]', expected_result=False)

    isalnum_verifier.verify('︳', expected_result=False)

    isalnum_verifier.verify('-', expected_result=False)

    isalnum_verifier.verify('%', expected_result=False)

    isalnum_verifier.verify('\n', expected_result=False)

    isalnum_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isalnum_verifier.verify('\t', expected_result=False)
    isalnum_verifier.verify(' ', expected_result=False)

    isalnum_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isalnum_verifier.verify('\u0000', expected_result=False)


def test_isalpha():
    def isalpha(tested: str) -> bool:
        return tested.isalpha()

    isalpha_verifier = verifier_for(isalpha)

    isalpha_verifier.verify('', expected_result=False)

    isalpha_verifier.verify('abc', expected_result=True)
    isalpha_verifier.verify('αβγ', expected_result=True)

    isalpha_verifier.verify('ABC', expected_result=True)
    isalpha_verifier.verify('ΑΒΓ', expected_result=True)

    isalpha_verifier.verify('ǋǲᾩ', expected_result=True)

    isalpha_verifier.verify('ᐎᐐᐊ', expected_result=True)

    isalpha_verifier.verify('ʷʰʸ', expected_result=True)

    isalpha_verifier.verify('123', expected_result=False)
    isalpha_verifier.verify('౧౨౩', expected_result=False)

    isalpha_verifier.verify('ⅠⅡⅢ', expected_result=False)
    isalpha_verifier.verify('¼½¾', expected_result=False)

    isalpha_verifier.verify('ABC123', expected_result=False)
    isalpha_verifier.verify('αβγ౧౨౩', expected_result=False)

    isalpha_verifier.verify('+', expected_result=False)
    isalpha_verifier.verify('±', expected_result=False)
    isalpha_verifier.verify('×', expected_result=False)

    isalpha_verifier.verify('©', expected_result=False)

    isalpha_verifier.verify('[]', expected_result=False)

    isalpha_verifier.verify('︳', expected_result=False)

    isalpha_verifier.verify('-', expected_result=False)

    isalpha_verifier.verify('%', expected_result=False)

    isalpha_verifier.verify('\n', expected_result=False)

    isalpha_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isalpha_verifier.verify('\t', expected_result=False)
    isalpha_verifier.verify(' ', expected_result=False)

    isalpha_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isalpha_verifier.verify('\u0000', expected_result=False)


def test_isascii():
    def isascii(tested: str) -> bool:
        return tested.isascii()

    isascii_verifier = verifier_for(isascii)

    isascii_verifier.verify('', expected_result=True)

    isascii_verifier.verify('abc', expected_result=True)
    isascii_verifier.verify('αβγ', expected_result=False)

    isascii_verifier.verify('ABC', expected_result=True)
    isascii_verifier.verify('ΑΒΓ', expected_result=False)

    isascii_verifier.verify('ǋǲᾩ', expected_result=False)

    isascii_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isascii_verifier.verify('ʷʰʸ', expected_result=False)

    isascii_verifier.verify('123', expected_result=True)
    isascii_verifier.verify('౧౨౩', expected_result=False)

    isascii_verifier.verify('ⅠⅡⅢ', expected_result=False)
    isascii_verifier.verify('¼½¾', expected_result=False)

    isascii_verifier.verify('ABC123', expected_result=True)
    isascii_verifier.verify('αβγ౧౨౩', expected_result=False)

    isascii_verifier.verify('+', expected_result=True)
    isascii_verifier.verify('±', expected_result=False)
    isascii_verifier.verify('×', expected_result=False)

    isascii_verifier.verify('©', expected_result=False)

    isascii_verifier.verify('[]', expected_result=True)

    isascii_verifier.verify('︳', expected_result=False)

    isascii_verifier.verify('-', expected_result=True)

    isascii_verifier.verify('%', expected_result=True)

    isascii_verifier.verify('\n', expected_result=True)

    isascii_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isascii_verifier.verify('\t', expected_result=True)
    isascii_verifier.verify(' ', expected_result=True)

    isascii_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isascii_verifier.verify('\u0000', expected_result=True)


def test_isdecimal():
    def isdecimal(tested: str) -> bool:
        return tested.isdecimal()

    isdecimal_verifier = verifier_for(isdecimal)

    isdecimal_verifier.verify('', expected_result=False)

    isdecimal_verifier.verify('abc', expected_result=False)
    isdecimal_verifier.verify('αβγ', expected_result=False)

    isdecimal_verifier.verify('ABC', expected_result=False)
    isdecimal_verifier.verify('ΑΒΓ', expected_result=False)

    isdecimal_verifier.verify('ǋǲᾩ', expected_result=False)

    isdecimal_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isdecimal_verifier.verify('ʷʰʸ', expected_result=False)

    isdecimal_verifier.verify('123', expected_result=True)
    isdecimal_verifier.verify('౧౨౩', expected_result=True)
    isdecimal_verifier.verify('\u00B2', expected_result=False)  # superscript 2

    isdecimal_verifier.verify('ⅠⅡⅢ', expected_result=False)
    isdecimal_verifier.verify('¼½¾', expected_result=False)

    isdecimal_verifier.verify('ABC123', expected_result=False)
    isdecimal_verifier.verify('αβγ౧౨౩', expected_result=False)

    isdecimal_verifier.verify('+', expected_result=False)
    isdecimal_verifier.verify('±', expected_result=False)
    isdecimal_verifier.verify('×', expected_result=False)

    isdecimal_verifier.verify('©', expected_result=False)

    isdecimal_verifier.verify('[]', expected_result=False)

    isdecimal_verifier.verify('︳', expected_result=False)

    isdecimal_verifier.verify('-', expected_result=False)

    isdecimal_verifier.verify('%', expected_result=False)

    isdecimal_verifier.verify('\n', expected_result=False)

    isdecimal_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isdecimal_verifier.verify('\t', expected_result=False)
    isdecimal_verifier.verify(' ', expected_result=False)

    isdecimal_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isdecimal_verifier.verify('\u0000', expected_result=False)


def test_isdigit():
    def isdigit(tested: str) -> bool:
        return tested.isdigit()

    isdigit_verifier = verifier_for(isdigit)

    isdigit_verifier.verify('', expected_result=False)

    isdigit_verifier.verify('abc', expected_result=False)
    isdigit_verifier.verify('αβγ', expected_result=False)

    isdigit_verifier.verify('ABC', expected_result=False)
    isdigit_verifier.verify('ΑΒΓ', expected_result=False)

    isdigit_verifier.verify('ǋǲᾩ', expected_result=False)

    isdigit_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isdigit_verifier.verify('ʷʰʸ', expected_result=False)

    isdigit_verifier.verify('123', expected_result=True)
    isdigit_verifier.verify('౧౨౩', expected_result=True)
    isdigit_verifier.verify('\u00B2', expected_result=True)  # superscript 2

    isdigit_verifier.verify('ⅠⅡⅢ', expected_result=False)
    isdigit_verifier.verify('¼½¾', expected_result=False)

    isdigit_verifier.verify('ABC123', expected_result=False)
    isdigit_verifier.verify('αβγ౧౨౩', expected_result=False)

    isdigit_verifier.verify('+', expected_result=False)
    isdigit_verifier.verify('±', expected_result=False)
    isdigit_verifier.verify('×', expected_result=False)

    isdigit_verifier.verify('©', expected_result=False)

    isdigit_verifier.verify('[]', expected_result=False)

    isdigit_verifier.verify('︳', expected_result=False)

    isdigit_verifier.verify('-', expected_result=False)

    isdigit_verifier.verify('%', expected_result=False)

    isdigit_verifier.verify('\n', expected_result=False)

    isdigit_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isdigit_verifier.verify('\t', expected_result=False)
    isdigit_verifier.verify(' ', expected_result=False)

    isdigit_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isdigit_verifier.verify('\u0000', expected_result=False)


def test_isidentifier():
    def isidentifier(tested: str) -> bool:
        return tested.isidentifier()

    isidentifier_verifier = verifier_for(isidentifier)

    isidentifier_verifier.verify('', expected_result=False)

    isidentifier_verifier.verify('abc', expected_result=True)
    isidentifier_verifier.verify('αβγ', expected_result=True)

    isidentifier_verifier.verify('ABC', expected_result=True)
    isidentifier_verifier.verify('ΑΒΓ', expected_result=True)

    isidentifier_verifier.verify('ǋǲᾩ', expected_result=True)

    isidentifier_verifier.verify('ᐎᐐᐊ', expected_result=True)

    isidentifier_verifier.verify('ʷʰʸ', expected_result=True)

    isidentifier_verifier.verify('123', expected_result=False)
    isidentifier_verifier.verify('౧౨౩', expected_result=False)

    isidentifier_verifier.verify('ⅠⅡⅢ', expected_result=True)
    isidentifier_verifier.verify('¼½¾', expected_result=False)

    isidentifier_verifier.verify('123abc', expected_result=False)
    isidentifier_verifier.verify('ABC123', expected_result=True)
    isidentifier_verifier.verify('αβγ౧౨౩', expected_result=True)

    isidentifier_verifier.verify('+', expected_result=False)
    isidentifier_verifier.verify('±', expected_result=False)
    isidentifier_verifier.verify('×', expected_result=False)

    isidentifier_verifier.verify('©', expected_result=False)

    isidentifier_verifier.verify('[]', expected_result=False)

    isidentifier_verifier.verify('︳', expected_result=False)

    isidentifier_verifier.verify('-', expected_result=False)

    isidentifier_verifier.verify('%', expected_result=False)

    isidentifier_verifier.verify('\n', expected_result=False)

    isidentifier_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isidentifier_verifier.verify('\t', expected_result=False)
    isidentifier_verifier.verify(' ', expected_result=False)

    isidentifier_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isidentifier_verifier.verify('\u0000', expected_result=False)


def test_islower():
    def islower(tested: str) -> bool:
        return tested.islower()

    islower_verifier = verifier_for(islower)

    islower_verifier.verify('', expected_result=False)

    islower_verifier.verify('abc', expected_result=True)
    islower_verifier.verify('αβγ', expected_result=True)

    islower_verifier.verify('ABC', expected_result=False)
    islower_verifier.verify('ΑΒΓ', expected_result=False)

    islower_verifier.verify('ǋǲᾩ', expected_result=False)

    islower_verifier.verify('ᐎᐐᐊ', expected_result=False)

    islower_verifier.verify('ʷʰʸ', expected_result=True)

    islower_verifier.verify('123', expected_result=False)
    islower_verifier.verify('౧౨౩', expected_result=False)

    islower_verifier.verify('ⅠⅡⅢ', expected_result=False)
    islower_verifier.verify('¼½¾', expected_result=False)

    islower_verifier.verify('ABC123', expected_result=False)
    islower_verifier.verify('αβγ౧౨౩', expected_result=True)

    islower_verifier.verify('+', expected_result=False)
    islower_verifier.verify('±', expected_result=False)
    islower_verifier.verify('×', expected_result=False)

    islower_verifier.verify('©', expected_result=False)

    islower_verifier.verify('[]', expected_result=False)

    islower_verifier.verify('︳', expected_result=False)

    islower_verifier.verify('-', expected_result=False)

    islower_verifier.verify('%', expected_result=False)

    islower_verifier.verify('\n', expected_result=False)

    islower_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    islower_verifier.verify('\t', expected_result=False)
    islower_verifier.verify(' ', expected_result=False)

    islower_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    islower_verifier.verify('\u0000', expected_result=False)


def test_isnumeric():
    def isnumeric(tested: str) -> bool:
        return tested.isnumeric()

    isnumeric_verifier = verifier_for(isnumeric)

    isnumeric_verifier.verify('', expected_result=False)

    isnumeric_verifier.verify('abc', expected_result=False)
    isnumeric_verifier.verify('αβγ', expected_result=False)

    isnumeric_verifier.verify('ABC', expected_result=False)
    isnumeric_verifier.verify('ΑΒΓ', expected_result=False)

    isnumeric_verifier.verify('ǋǲᾩ', expected_result=False)

    isnumeric_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isnumeric_verifier.verify('ʷʰʸ', expected_result=False)

    isnumeric_verifier.verify('123', expected_result=True)
    isnumeric_verifier.verify('౧౨౩', expected_result=True)

    isnumeric_verifier.verify('ⅠⅡⅢ', expected_result=True)
    isnumeric_verifier.verify('¼½¾', expected_result=True)

    isnumeric_verifier.verify('ABC123', expected_result=False)
    isnumeric_verifier.verify('αβγ౧౨౩', expected_result=False)

    isnumeric_verifier.verify('+', expected_result=False)
    isnumeric_verifier.verify('±', expected_result=False)
    isnumeric_verifier.verify('×', expected_result=False)

    isnumeric_verifier.verify('©', expected_result=False)

    isnumeric_verifier.verify('[]', expected_result=False)

    isnumeric_verifier.verify('︳', expected_result=False)

    isnumeric_verifier.verify('-', expected_result=False)

    isnumeric_verifier.verify('%', expected_result=False)

    isnumeric_verifier.verify('\n', expected_result=False)

    isnumeric_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isnumeric_verifier.verify('\t', expected_result=False)
    isnumeric_verifier.verify(' ', expected_result=False)

    isnumeric_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isnumeric_verifier.verify('\u0000', expected_result=False)


def test_isprintable():
    def isprintable(tested: str) -> bool:
        return tested.isprintable()

    isprintable_verifier = verifier_for(isprintable)

    isprintable_verifier.verify('', expected_result=True)

    isprintable_verifier.verify('abc', expected_result=True)
    isprintable_verifier.verify('αβγ', expected_result=True)

    isprintable_verifier.verify('ABC', expected_result=True)
    isprintable_verifier.verify('ΑΒΓ', expected_result=True)

    isprintable_verifier.verify('ǋǲᾩ', expected_result=True)

    isprintable_verifier.verify('ᐎᐐᐊ', expected_result=True)

    isprintable_verifier.verify('ʷʰʸ', expected_result=True)

    isprintable_verifier.verify('123', expected_result=True)
    isprintable_verifier.verify('౧౨౩', expected_result=True)

    isprintable_verifier.verify('ⅠⅡⅢ', expected_result=True)
    isprintable_verifier.verify('¼½¾', expected_result=True)

    isprintable_verifier.verify('ABC123', expected_result=True)
    isprintable_verifier.verify('αβγ౧౨౩', expected_result=True)

    isprintable_verifier.verify('+', expected_result=True)
    isprintable_verifier.verify('±', expected_result=True)
    isprintable_verifier.verify('×', expected_result=True)

    isprintable_verifier.verify('©', expected_result=True)

    isprintable_verifier.verify('[]', expected_result=True)

    isprintable_verifier.verify('︳', expected_result=True)

    isprintable_verifier.verify('-', expected_result=True)

    isprintable_verifier.verify('%', expected_result=True)

    isprintable_verifier.verify('\n', expected_result=False)

    isprintable_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isprintable_verifier.verify('\t', expected_result=False)
    isprintable_verifier.verify(' ', expected_result=True)

    isprintable_verifier.verify('\U0001f64f', expected_result=True)   # Need surrogates in UTF-16

    isprintable_verifier.verify('\u0000', expected_result=False)


def test_isspace():
    def isspace(tested: str) -> bool:
        return tested.isspace()

    isspace_verifier = verifier_for(isspace)

    isspace_verifier.verify('', expected_result=False)

    isspace_verifier.verify('abc', expected_result=False)
    isspace_verifier.verify('αβγ', expected_result=False)

    isspace_verifier.verify('ABC', expected_result=False)
    isspace_verifier.verify('ΑΒΓ', expected_result=False)

    isspace_verifier.verify('ǋǲᾩ', expected_result=False)

    isspace_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isspace_verifier.verify('ʷʰʸ', expected_result=False)

    isspace_verifier.verify('123', expected_result=False)
    isspace_verifier.verify('౧౨౩', expected_result=False)

    isspace_verifier.verify('ⅠⅡⅢ', expected_result=False)
    isspace_verifier.verify('¼½¾', expected_result=False)

    isspace_verifier.verify('ABC123', expected_result=False)
    isspace_verifier.verify('αβγ౧౨౩', expected_result=False)

    isspace_verifier.verify('+', expected_result=False)
    isspace_verifier.verify('±', expected_result=False)
    isspace_verifier.verify('×', expected_result=False)

    isspace_verifier.verify('©', expected_result=False)

    isspace_verifier.verify('[]', expected_result=False)

    isspace_verifier.verify('︳', expected_result=False)

    isspace_verifier.verify('-', expected_result=False)

    isspace_verifier.verify('%', expected_result=False)

    isspace_verifier.verify('\n', expected_result=True)

    isspace_verifier.verify('\u2029', expected_result=True)  # paragraph seperator

    isspace_verifier.verify('\t', expected_result=True)
    isspace_verifier.verify(' ', expected_result=True)

    isspace_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isspace_verifier.verify('\u0000', expected_result=False)


def test_istitle():
    def istitle(tested: str) -> bool:
        return tested.istitle()

    istitle_verifier = verifier_for(istitle)

    istitle_verifier.verify('', expected_result=False)

    istitle_verifier.verify('Abc', expected_result=True)
    istitle_verifier.verify('Abc Αβγ', expected_result=True)
    istitle_verifier.verify('Abc αβγ', expected_result=False)

    istitle_verifier.verify('abc', expected_result=False)
    istitle_verifier.verify('αβγ', expected_result=False)

    istitle_verifier.verify('ABC', expected_result=False)
    istitle_verifier.verify('ΑΒΓ', expected_result=False)

    istitle_verifier.verify('ǋǲᾩ', expected_result=False)

    istitle_verifier.verify('ᐎᐐᐊ', expected_result=False)

    istitle_verifier.verify('ʷʰʸ', expected_result=False)

    istitle_verifier.verify('123', expected_result=False)
    istitle_verifier.verify('౧౨౩', expected_result=False)

    istitle_verifier.verify('ⅠⅡⅢ', expected_result=False)
    istitle_verifier.verify('¼½¾', expected_result=False)

    istitle_verifier.verify('ABC123', expected_result=False)
    istitle_verifier.verify('αβγ౧౨౩', expected_result=False)

    istitle_verifier.verify('+', expected_result=False)
    istitle_verifier.verify('±', expected_result=False)
    istitle_verifier.verify('×', expected_result=False)

    istitle_verifier.verify('©', expected_result=False)

    istitle_verifier.verify('[]', expected_result=False)

    istitle_verifier.verify('︳', expected_result=False)

    istitle_verifier.verify('-', expected_result=False)

    istitle_verifier.verify('%', expected_result=False)

    istitle_verifier.verify('\n', expected_result=False)

    istitle_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    istitle_verifier.verify('\t', expected_result=False)
    istitle_verifier.verify(' ', expected_result=False)

    istitle_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    istitle_verifier.verify('\u0000', expected_result=False)


def test_isupper():
    def isupper(tested: str) -> bool:
        return tested.isupper()

    isupper_verifier = verifier_for(isupper)

    isupper_verifier.verify('', expected_result=False)

    isupper_verifier.verify('abc', expected_result=False)
    isupper_verifier.verify('αβγ', expected_result=False)

    isupper_verifier.verify('ABC', expected_result=True)
    isupper_verifier.verify('ΑΒΓ', expected_result=True)

    isupper_verifier.verify('ǋǲᾩ', expected_result=False)

    isupper_verifier.verify('ᐎᐐᐊ', expected_result=False)

    isupper_verifier.verify('ʷʰʸ', expected_result=False)

    isupper_verifier.verify('123', expected_result=False)
    isupper_verifier.verify('౧౨౩', expected_result=False)

    isupper_verifier.verify('ⅠⅡⅢ', expected_result=True)
    isupper_verifier.verify('¼½¾', expected_result=False)

    isupper_verifier.verify('ABC123', expected_result=True)
    isupper_verifier.verify('αβγ౧౨౩', expected_result=False)

    isupper_verifier.verify('+', expected_result=False)
    isupper_verifier.verify('±', expected_result=False)
    isupper_verifier.verify('×', expected_result=False)

    isupper_verifier.verify('©', expected_result=False)

    isupper_verifier.verify('[]', expected_result=False)

    isupper_verifier.verify('︳', expected_result=False)

    isupper_verifier.verify('-', expected_result=False)

    isupper_verifier.verify('%', expected_result=False)

    isupper_verifier.verify('\n', expected_result=False)

    isupper_verifier.verify('\u2029', expected_result=False)  # paragraph seperator

    isupper_verifier.verify('\t', expected_result=False)
    isupper_verifier.verify(' ', expected_result=False)

    isupper_verifier.verify('\U0001f64f', expected_result=False)   # Need surrogates in UTF-16

    isupper_verifier.verify('\u0000', expected_result=False)


def test_join():
    def join(tested: str, iterable: list) -> str:
        return tested.join(iterable)

    join_verifier = verifier_for(join)

    join_verifier.verify(', ', [], expected_result='')
    join_verifier.verify(', ', ['a'], expected_result='a')
    join_verifier.verify(', ', ['a', 'b'], expected_result='a, b')
    join_verifier.verify(' ', ['hello', 'world', 'again'], expected_result='hello world again')
    join_verifier.verify('\n', ['1', '2', '3'], expected_result='1\n2\n3')
    join_verifier.verify(', ', [1, 2], expected_error=TypeError)


def test_ljust():
    def ljust(tested: str, width: int) -> str:
        return tested.ljust(width)

    def ljust_with_fill(tested: str, width: int, fill: str) -> str:
        return tested.ljust(width, fill)

    ljust_verifier = verifier_for(ljust)
    ljust_with_fill_verifier = verifier_for(ljust_with_fill)

    ljust_verifier.verify('test', 10, expected_result='test      ')
    ljust_verifier.verify('test', 9, expected_result='test     ')
    ljust_verifier.verify('test', 4, expected_result='test')
    ljust_verifier.verify('test', 2, expected_result='test')

    ljust_with_fill_verifier.verify('test', 10, '#', expected_result='test######')
    ljust_with_fill_verifier.verify('test', 9, '#', expected_result='test#####')
    ljust_with_fill_verifier.verify('test', 4, '#', expected_result='test')
    ljust_with_fill_verifier.verify('test', 2, '#', expected_result='test')


def test_lower():
    def lower(tested: str) -> str:
        return tested.lower()

    lower_verifier = verifier_for(lower)

    lower_verifier.verify('', expected_result='')

    lower_verifier.verify('abc', expected_result='abc')
    lower_verifier.verify('αβγ', expected_result='αβγ')

    lower_verifier.verify('ABC', expected_result='abc')
    lower_verifier.verify('ΑΒΓ', expected_result='αβγ')

    lower_verifier.verify('ǋǲᾩ', expected_result='ǌǳᾡ')

    lower_verifier.verify('ᐎᐐᐊ', expected_result='ᐎᐐᐊ')

    lower_verifier.verify('ʷʰʸ', expected_result='ʷʰʸ')

    lower_verifier.verify('123', expected_result='123')
    lower_verifier.verify('౧౨౩', expected_result='౧౨౩')

    lower_verifier.verify('ⅠⅡⅢ', expected_result='ⅰⅱⅲ')
    lower_verifier.verify('¼½¾', expected_result='¼½¾')

    lower_verifier.verify('ABC123', expected_result='abc123')
    lower_verifier.verify('αβγ౧౨౩', expected_result='αβγ౧౨౩')

    lower_verifier.verify('+', expected_result='+')
    lower_verifier.verify('±', expected_result='±')
    lower_verifier.verify('×', expected_result='×')

    lower_verifier.verify('©', expected_result='©')

    lower_verifier.verify('[]', expected_result='[]')

    lower_verifier.verify('︳', expected_result='︳')

    lower_verifier.verify('-', expected_result='-')

    lower_verifier.verify('%', expected_result='%')

    lower_verifier.verify('\n', expected_result='\n')

    lower_verifier.verify('\u2029', expected_result='\u2029')  # paragraph seperator

    lower_verifier.verify('\t', expected_result='\t')
    lower_verifier.verify(' ', expected_result=' ')

    lower_verifier.verify('\U0001f64f', expected_result='\U0001f64f')   # Need surrogates in UTF-16

    # Fails due to https://github.com/jpype-project/jpype/issues/1091
    # lower_verifier.verify('\u0000', expected_result='\u0000')


def test_lstrip():
    def lstrip(tested: str) -> str:
        return tested.lstrip()

    def lstrip_with_chars(tested: str, chars: str) -> str:
        return tested.lstrip(chars)

    lstrip_verifier = verifier_for(lstrip)
    lstrip_with_chars_verifier = verifier_for(lstrip_with_chars)

    lstrip_verifier.verify('   spacious   ', expected_result='spacious   ')
    lstrip_with_chars_verifier.verify('www.example.com', 'cmowz.', expected_result='example.com')


def test_partition():
    def partition(tested: str, sep: str) -> tuple:
        return tested.partition(sep)

    partition_verifier = verifier_for(partition)

    partition_verifier.verify('before+after+extra', '+', expected_result=('before', '+', 'after+extra'))
    partition_verifier.verify('before and after and extra', '+', expected_result=('before and after and extra', '', ''))
    partition_verifier.verify('before and after and extra', ' and ',
                              expected_result=('before', ' and ', 'after and extra'))
    partition_verifier.verify('before+after+extra', ' and ', expected_result=('before+after+extra', '', ''))


def test_removeprefix():
    def removeprefix(tested: str, prefix: str) -> str:
        return tested.removeprefix(prefix)

    removeprefix_verifier = verifier_for(removeprefix)

    removeprefix_verifier.verify('TestHook', 'Test', expected_result='Hook')
    removeprefix_verifier.verify('BaseTestCase', 'Test', expected_result='BaseTestCase')
    removeprefix_verifier.verify('BaseCaseTest', 'Test', expected_result='BaseCaseTest')
    removeprefix_verifier.verify('BaseCase', 'Test', expected_result='BaseCase')


def test_removesuffix():
    def removesuffix(tested: str, prefix: str) -> str:
        return tested.removesuffix(prefix)

    removesuffix_verifier = verifier_for(removesuffix)

    removesuffix_verifier.verify('MiscTests', 'Tests', expected_result='Misc')
    removesuffix_verifier.verify('TmpTestsDirMixin', 'Tests', expected_result='TmpTestsDirMixin')
    removesuffix_verifier.verify('TestsTmpDirMixin', 'Tests', expected_result='TestsTmpDirMixin')
    removesuffix_verifier.verify('TmpDirMixin', 'Tests', expected_result='TmpDirMixin')


def test_replace():
    def replace(tested: str, substring: str, replacement: str) -> str:
        return tested.replace(substring, replacement)

    def replace_with_count(tested: str, substring: str, replacement: str, count: int) -> str:
        return tested.replace(substring, replacement, count)


    replace_verifier = verifier_for(replace)
    replace_with_count_verifier = verifier_for(replace_with_count)

    replace_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog',
                            expected_result='all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', 0,
                                        expected_result='all cats, including the cat Alcato, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', 1,
                                       expected_result='all dogs, including the cat Alcato, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', 2,
                                       expected_result='all dogs, including the dog Alcato, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', 3,
                                       expected_result='all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', 4,
                                       expected_result='all dogs, including the dog Aldogo, are animals')
    replace_with_count_verifier.verify('all cats, including the cat Alcato, are animals', 'cat', 'dog', -1,
                                       expected_result='all dogs, including the dog Aldogo, are animals')


def test_rfind():
    def rfind(tested: str, item: str) -> int:
        return tested.rfind(item)

    def rfind_start_verifier(tested: str, item: str, start: int) -> int:
        return tested.rfind(item, start)

    def rfind_start_end_verifier(tested: str, item: str, start: int, end: int) -> int:
        return tested.rfind(item, start, end)

    rfind_verifier = verifier_for(rfind)
    rfind_start_verifier = verifier_for(rfind_start_verifier)
    rfind_start_end_verifier = verifier_for(rfind_start_end_verifier)

    rfind_verifier.verify('abcabc', 'a', expected_result=3)
    rfind_verifier.verify('abcabc', 'b', expected_result=4)
    rfind_verifier.verify('abcabc', 'd', expected_result=-1)

    rfind_start_verifier.verify('abcabc', 'a', 1, expected_result=3)
    rfind_start_verifier.verify('abcabc', 'a', 5, expected_result=-1)
    rfind_start_verifier.verify('abcabc', 'b', 1, expected_result=4)
    rfind_start_verifier.verify('abcabc', 'c', 1, expected_result=5)
    rfind_start_verifier.verify('abcabc', 'd', 1, expected_result=-1)

    rfind_start_verifier.verify('abcabc', 'a', -3, expected_result=3)
    rfind_start_verifier.verify('abcabc', 'b', -2, expected_result=4)
    rfind_start_verifier.verify('abcabc', 'c', -2, expected_result=5)
    rfind_start_verifier.verify('abcabc', 'd', -2, expected_result=-1)

    rfind_start_end_verifier.verify('abcabc', 'a', 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify('abcabc', 'b', 1, 2, expected_result=1)
    rfind_start_end_verifier.verify('abcabc', 'c', 1, 2, expected_result=-1)
    rfind_start_end_verifier.verify('abcabc', 'd', 1, 2, expected_result=-1)

    rfind_start_end_verifier.verify('abcabc', 'a', -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify('abcabc', 'b', -2, -1, expected_result=4)
    rfind_start_end_verifier.verify('abcabc', 'c', -2, -1, expected_result=-1)
    rfind_start_end_verifier.verify('abcabc', 'd', -2, -1, expected_result=-1)


def test_rindex():
    def rindex(tested: str, item: str) -> int:
        return tested.rindex(item)

    def rindex_start_verifier(tested: str, item: str, start: int) -> int:
        return tested.rindex(item, start)

    def rindex_start_end_verifier(tested: str, item: str, start: int, end: int) -> int:
        return tested.rindex(item, start, end)

    rindex_verifier = verifier_for(rindex)
    rindex_start_verifier = verifier_for(rindex_start_verifier)
    rindex_start_end_verifier = verifier_for(rindex_start_end_verifier)

    rindex_verifier.verify('abcabc', 'a', expected_result=3)
    rindex_verifier.verify('abcabc', 'b', expected_result=4)
    rindex_verifier.verify('abcabc', 'd', expected_error=ValueError)

    rindex_start_verifier.verify('abcabc', 'a', 1, expected_result=3)
    rindex_start_verifier.verify('abcabc', 'a', 5, expected_error=ValueError)
    rindex_start_verifier.verify('abcabc', 'b', 1, expected_result=4)
    rindex_start_verifier.verify('abcabc', 'c', 1, expected_result=5)
    rindex_start_verifier.verify('abcabc', 'd', 1, expected_error=ValueError)

    rindex_start_verifier.verify('abcabc', 'a', -3, expected_result=3)
    rindex_start_verifier.verify('abcabc', 'b', -2, expected_result=4)
    rindex_start_verifier.verify('abcabc', 'c', -2, expected_result=5)
    rindex_start_verifier.verify('abcabc', 'd', -2, expected_error=ValueError)

    rindex_start_end_verifier.verify('abcabc', 'a', 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify('abcabc', 'b', 1, 2, expected_result=1)
    rindex_start_end_verifier.verify('abcabc', 'c', 1, 2, expected_error=ValueError)
    rindex_start_end_verifier.verify('abcabc', 'd', 1, 2, expected_error=ValueError)

    rindex_start_end_verifier.verify('abcabc', 'a', -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify('abcabc', 'b', -2, -1, expected_result=4)
    rindex_start_end_verifier.verify('abcabc', 'c', -2, -1, expected_error=ValueError)
    rindex_start_end_verifier.verify('abcabc', 'd', -2, -1, expected_error=ValueError)

def test_rjust():
    def rjust(tested: str, width: int) -> str:
        return tested.rjust(width)

    def rjust_with_fill(tested: str, width: int, fill: str) -> str:
        return tested.rjust(width, fill)

    rjust_verifier = verifier_for(rjust)
    rjust_with_fill_verifier = verifier_for(rjust_with_fill)

    rjust_verifier.verify('test', 10, expected_result='      test')
    rjust_verifier.verify('test', 9, expected_result='     test')
    rjust_verifier.verify('test', 4, expected_result='test')
    rjust_verifier.verify('test', 2, expected_result='test')

    rjust_with_fill_verifier.verify('test', 10, '#', expected_result='######test')
    rjust_with_fill_verifier.verify('test', 9, '#', expected_result='#####test')
    rjust_with_fill_verifier.verify('test', 4, '#', expected_result='test')
    rjust_with_fill_verifier.verify('test', 2, '#', expected_result='test')


def test_rpartition():
    def rpartition(tested: str, sep: str) -> tuple:
        return tested.rpartition(sep)

    rpartition_verifier = verifier_for(rpartition)

    rpartition_verifier.verify('before+after+extra', '+', expected_result=('before+after', '+', 'extra'))
    rpartition_verifier.verify('before and after and extra', '+', expected_result=('', '', 'before and after and extra'))
    rpartition_verifier.verify('before and after and extra', ' and ',
                              expected_result=('before and after', ' and ', 'extra'))
    rpartition_verifier.verify('before+after+extra', ' and ', expected_result=('', '', 'before+after+extra'))


def test_rsplit():
    def rsplit(tested: str) -> list:
        return tested.rsplit()

    def rsplit_with_sep(tested: str, sep: str) -> list:
        return tested.rsplit(sep)

    def rsplit_with_sep_and_count(tested: str, sep: str, count: int) -> list:
        return tested.rsplit(sep, count)

    rsplit_verifier = verifier_for(rsplit)
    rsplit_with_sep_verifier = verifier_for(rsplit_with_sep)
    rsplit_with_sep_and_count_verifier = verifier_for(rsplit_with_sep_and_count)

    rsplit_verifier.verify('123', expected_result=['123'])
    rsplit_verifier.verify('1 2 3', expected_result=['1', '2', '3'])
    rsplit_verifier.verify(' 1 2 3 ', expected_result=['1', '2', '3'])
    rsplit_verifier.verify('1\n2\n3', expected_result=['1', '2', '3'])
    rsplit_verifier.verify('1\t2\t3', expected_result=['1', '2', '3'])

    rsplit_with_sep_verifier.verify('1,2,3', ',', expected_result=['1', '2', '3'])
    rsplit_with_sep_verifier.verify('1,2,,3,', ',', expected_result=['1', '2', '', '3', ''])
    rsplit_with_sep_verifier.verify(',1,2,,3,', ',', expected_result=['', '1', '2', '', '3', ''])

    rsplit_with_sep_and_count_verifier.verify('1,2,3', ',', 1, expected_result=['1,2', '3'])
    rsplit_with_sep_and_count_verifier.verify('1,2,,3,', ',', 1, expected_result=['1,2,,3', ''])
    rsplit_with_sep_and_count_verifier.verify('1,2,,3,', ',', 2, expected_result=['1,2,', '3', ''])
    rsplit_with_sep_and_count_verifier.verify('1,2,,3,', ',', 3, expected_result=['1,2', '',  '3', ''])
    rsplit_with_sep_and_count_verifier.verify('1,2,,3,', ',', 4, expected_result=['1', '2', '',  '3', ''])


def test_rstrip():
    def rstrip(tested: str) -> str:
        return tested.rstrip()

    def rstrip_with_chars(tested: str, chars: str) -> str:
        return tested.rstrip(chars)

    rstrip_verifier = verifier_for(rstrip)
    rstrip_with_chars_verifier = verifier_for(rstrip_with_chars)

    rstrip_verifier.verify('   spacious   ', expected_result='   spacious')
    rstrip_with_chars_verifier.verify('www.example.com', 'cmowz.', expected_result='www.example')


def test_split():
    def split(tested: str) -> list:
        return tested.split()

    def split_with_sep(tested: str, sep: str) -> list:
        return tested.split(sep)

    def split_with_sep_and_count(tested: str, sep: str, count: int) -> list:
        return tested.split(sep, count)

    split_verifier = verifier_for(split)
    split_with_sep_verifier = verifier_for(split_with_sep)
    split_with_sep_and_count_verifier = verifier_for(split_with_sep_and_count)

    split_verifier.verify('123', expected_result=['123'])
    split_verifier.verify('1 2 3', expected_result=['1', '2', '3'])
    split_verifier.verify(' 1 2 3 ', expected_result=['1', '2', '3'])
    split_verifier.verify('1\n2\n3', expected_result=['1', '2', '3'])
    split_verifier.verify('1\t2\t3', expected_result=['1', '2', '3'])

    split_with_sep_verifier.verify('1,2,3', ',', expected_result=['1', '2', '3'])
    split_with_sep_verifier.verify('1,2,,3,', ',', expected_result=['1', '2', '', '3', ''])
    split_with_sep_verifier.verify(',1,2,,3,', ',', expected_result=['', '1', '2', '', '3', ''])

    split_with_sep_and_count_verifier.verify('1,2,3', ',', 1, expected_result=['1', '2,3'])
    split_with_sep_and_count_verifier.verify('1,2,,3,', ',', 1, expected_result=['1', '2,,3,'])
    split_with_sep_and_count_verifier.verify('1,2,,3,', ',', 2, expected_result=['1', '2', ',3,'])
    split_with_sep_and_count_verifier.verify('1,2,,3,', ',', 3, expected_result=['1', '2', '',  '3,'])
    split_with_sep_and_count_verifier.verify('1,2,,3,', ',', 4, expected_result=['1', '2', '',  '3', ''])


def test_splitlines():
    def splitlines(tested: str) -> list:
        return tested.splitlines()

    def splitlines_keep_ends(tested: str, keep_ends: bool) -> list:
        return tested.splitlines(keep_ends)

    splitlines_verifier = verifier_for(splitlines)
    splitlines_keep_ends_verifier = verifier_for(splitlines_keep_ends)

    splitlines_verifier.verify('ab c\n\nde fg\rkl\r\n', expected_result=['ab c', '', 'de fg', 'kl'])
    splitlines_verifier.verify('', expected_result=[])
    splitlines_verifier.verify('One line\n', expected_result=['One line'])

    splitlines_keep_ends_verifier.verify('ab c\n\nde fg\rkl\r\n', False, expected_result=['ab c', '', 'de fg', 'kl'])
    splitlines_keep_ends_verifier.verify('ab c\n\nde fg\rkl\r\n', True,
                                         expected_result=['ab c\n', '\n', 'de fg\r', 'kl\r\n'])
    splitlines_keep_ends_verifier.verify('', True, expected_result=[])
    splitlines_keep_ends_verifier.verify('', False, expected_result=[])
    splitlines_keep_ends_verifier.verify('One line\n', True, expected_result=['One line\n'])
    splitlines_keep_ends_verifier.verify('One line\n', False, expected_result=['One line'])


def test_startswith():
    def startswith(tested: str, suffix: str) -> bool:
        return tested.startswith(suffix)

    def startswith_start(tested: str, suffix: str, start: int) -> bool:
        return tested.startswith(suffix, start)

    def startswith_between(tested: str, suffix: str, start: int, end: int) -> bool:
        return tested.startswith(suffix, start, end)

    startswith_verifier = verifier_for(startswith)
    startswith_start_verifier = verifier_for(startswith_start)
    startswith_between_verifier = verifier_for(startswith_between)

    startswith_verifier.verify('hello world', 'hello', expected_result=True)
    startswith_verifier.verify('hello world', 'world', expected_result=False)
    startswith_verifier.verify('hello', 'hello world', expected_result=False)
    startswith_verifier.verify('hello world', 'hello world', expected_result=True)

    startswith_start_verifier.verify('hello world', 'world', 6, expected_result=True)
    startswith_start_verifier.verify('hello world', 'hello', 6, expected_result=False)
    startswith_start_verifier.verify('hello', 'hello world', 6, expected_result=False)
    startswith_start_verifier.verify('hello world', 'hello world', 6, expected_result=False)

    startswith_between_verifier.verify('hello world', 'world', 6, 11, expected_result=True)
    startswith_between_verifier.verify('hello world', 'world', 7, 11, expected_result=False)
    startswith_between_verifier.verify('hello world', 'hello', 0, 5, expected_result=True)
    startswith_between_verifier.verify('hello', 'hello world', 0, 5, expected_result=False)
    startswith_between_verifier.verify('hello world', 'hello world', 5, 11, expected_result=False)


def test_strip():
    def strip(tested: str) -> str:
        return tested.strip()

    def strip_with_chars(tested: str, chars: str) -> str:
        return tested.strip(chars)

    strip_verifier = verifier_for(strip)
    strip_with_chars_verifier = verifier_for(strip_with_chars)

    strip_verifier.verify('   spacious   ', expected_result='spacious')
    strip_with_chars_verifier.verify('www.example.com', 'cmowz.', expected_result='example')


def test_swapcase():
    def swapcase(tested: str) -> str:
        return tested.swapcase()

    swapcase_verifier = verifier_for(swapcase)

    swapcase_verifier.verify('', expected_result='')

    swapcase_verifier.verify('abc', expected_result='ABC')
    swapcase_verifier.verify('αβγ', expected_result='ΑΒΓ')

    swapcase_verifier.verify('ABC', expected_result='abc')
    swapcase_verifier.verify('ΑΒΓ', expected_result='αβγ')

    swapcase_verifier.verify('ǋǲᾩ', expected_result='ǋǲᾩ')

    swapcase_verifier.verify('ᐎᐐᐊ', expected_result='ᐎᐐᐊ')

    swapcase_verifier.verify('ʷʰʸ', expected_result='ʷʰʸ')

    swapcase_verifier.verify('123', expected_result='123')
    swapcase_verifier.verify('౧౨౩', expected_result='౧౨౩')

    swapcase_verifier.verify('ⅠⅡⅢ', expected_result='ⅰⅱⅲ')
    swapcase_verifier.verify('ⅰⅱⅲ', expected_result='ⅠⅡⅢ')
    swapcase_verifier.verify('¼½¾', expected_result='¼½¾')

    swapcase_verifier.verify('ABC123', expected_result='abc123')
    swapcase_verifier.verify('αβγ౧౨౩', expected_result='ΑΒΓ౧౨౩')

    swapcase_verifier.verify('+', expected_result='+')
    swapcase_verifier.verify('±', expected_result='±')
    swapcase_verifier.verify('×', expected_result='×')

    swapcase_verifier.verify('©', expected_result='©')

    swapcase_verifier.verify('[]', expected_result='[]')

    swapcase_verifier.verify('︳', expected_result='︳')

    swapcase_verifier.verify('-', expected_result='-')

    swapcase_verifier.verify('%', expected_result='%')

    swapcase_verifier.verify('\n', expected_result='\n')

    swapcase_verifier.verify('\u2029', expected_result='\u2029')  # paragraph seperator

    swapcase_verifier.verify('\t', expected_result='\t')
    swapcase_verifier.verify(' ', expected_result=' ')

    swapcase_verifier.verify('\U0001f64f', expected_result='\U0001f64f')   # Need surrogates in UTF-16

    # Fails due to https://github.com/jpype-project/jpype/issues/1091
    # swapcase_verifier.verify('\u0000', expected_result='\u0000')


def test_title():
    def title(tested: str) -> str:
        return tested.title()

    title_verifier = verifier_for(title)

    title_verifier.verify('', expected_result='')
    title_verifier.verify('Hello world', expected_result='Hello World')
    title_verifier.verify("they're bill's friends from the UK",
                              expected_result="They'Re Bill'S Friends From The Uk")

    title_verifier.verify('abc', expected_result='Abc')
    title_verifier.verify('αβγ', expected_result='Αβγ')

    title_verifier.verify('ABC', expected_result='Abc')
    title_verifier.verify('ΑΒΓ', expected_result='Αβγ')

    title_verifier.verify('ǋǲᾩ', expected_result='ǋǳᾡ')

    title_verifier.verify('ᐎᐐᐊ', expected_result='ᐎᐐᐊ')

    title_verifier.verify('ʷʰʸ', expected_result='ʷʰʸ')

    title_verifier.verify('123', expected_result='123')
    title_verifier.verify('౧౨౩', expected_result='౧౨౩')

    title_verifier.verify('ⅠⅡⅢ', expected_result='Ⅰⅱⅲ')
    title_verifier.verify('ⅰⅱⅲ', expected_result='Ⅰⅱⅲ')
    title_verifier.verify('¼½¾', expected_result='¼½¾')

    title_verifier.verify('ABC123', expected_result='Abc123')
    title_verifier.verify('αβγ౧౨౩', expected_result='Αβγ౧౨౩')

    title_verifier.verify('+', expected_result='+')
    title_verifier.verify('±', expected_result='±')
    title_verifier.verify('×', expected_result='×')

    title_verifier.verify('©', expected_result='©')

    title_verifier.verify('[]', expected_result='[]')

    title_verifier.verify('︳', expected_result='︳')

    title_verifier.verify('-', expected_result='-')

    title_verifier.verify('%', expected_result='%')

    title_verifier.verify('\n', expected_result='\n')

    title_verifier.verify('\u2029', expected_result='\u2029')  # paragraph seperator

    title_verifier.verify('\t', expected_result='\t')
    title_verifier.verify(' ', expected_result=' ')

    title_verifier.verify('\U0001f64f', expected_result='\U0001f64f')   # Need surrogates in UTF-16

    # Fails due to https://github.com/jpype-project/jpype/issues/1091
    # swapcase_verifier.verify('\u0000', expected_result='\u0000')


def test_translate():
    def translate(tested: str, mapping: dict) -> str:
        return tested.translate(mapping)

    translate_verifier = verifier_for(translate)

    translate_verifier.verify('hello world',
                              {
                                  ord('l'): '7',
                                  ord('h'): 'H',
                                  ord('w'): 'WO',
                                  ord('d'): ''
                              }, expected_result='He77o WOor7')


def test_upper():
    def upper(tested: str) -> str:
        return tested.upper()

    upper_verifier = verifier_for(upper)

    upper_verifier.verify('', expected_result='')

    upper_verifier.verify('abc', expected_result='ABC')
    upper_verifier.verify('αβγ', expected_result='ΑΒΓ')

    upper_verifier.verify('ABC', expected_result='ABC')
    upper_verifier.verify('ΑΒΓ', expected_result='ΑΒΓ')

    upper_verifier.verify('ǋǲᾩ', expected_result='ǊǱὩΙ')

    upper_verifier.verify('ᐎᐐᐊ', expected_result='ᐎᐐᐊ')

    upper_verifier.verify('ʷʰʸ', expected_result='ʷʰʸ')

    upper_verifier.verify('123', expected_result='123')
    upper_verifier.verify('౧౨౩', expected_result='౧౨౩')

    upper_verifier.verify('ⅰⅱⅲ', expected_result='ⅠⅡⅢ')
    upper_verifier.verify('ⅠⅡⅢ', expected_result='ⅠⅡⅢ')
    upper_verifier.verify('¼½¾', expected_result='¼½¾')

    upper_verifier.verify('ABC123', expected_result='ABC123')
    upper_verifier.verify('αβγ౧౨౩', expected_result='ΑΒΓ౧౨౩')

    upper_verifier.verify('+', expected_result='+')
    upper_verifier.verify('±', expected_result='±')
    upper_verifier.verify('×', expected_result='×')

    upper_verifier.verify('©', expected_result='©')

    upper_verifier.verify('[]', expected_result='[]')

    upper_verifier.verify('︳', expected_result='︳')

    upper_verifier.verify('-', expected_result='-')

    upper_verifier.verify('%', expected_result='%')

    upper_verifier.verify('\n', expected_result='\n')

    upper_verifier.verify('\u2029', expected_result='\u2029')  # paragraph seperator

    upper_verifier.verify('\t', expected_result='\t')
    upper_verifier.verify(' ', expected_result=' ')

    upper_verifier.verify('\U0001f64f', expected_result='\U0001f64f')   # Need surrogates in UTF-16

    # Fails due to https://github.com/jpype-project/jpype/issues/1091
    # lower_verifier.verify('\u0000', expected_result='\u0000')


def test_zfill():
    def zfill(tested: str, padding: int) -> str:
        return tested.zfill(padding)

    zfill_verifier = verifier_for(zfill)

    zfill_verifier.verify('42', 5, expected_result='00042')
    zfill_verifier.verify('-42', 5, expected_result='-0042')
    zfill_verifier.verify('+42', 5, expected_result='+0042')
    zfill_verifier.verify('42', 1, expected_result='42')
    zfill_verifier.verify('-42', 1, expected_result='-42')
    zfill_verifier.verify('+42', 1, expected_result='+42')
    zfill_verifier.verify('abc', 10, expected_result='0000000abc')
    zfill_verifier.verify('ᐎᐐᐊ', 5, expected_result='00ᐎᐐᐊ')
    zfill_verifier.verify('+ᐎᐐᐊ', 5, expected_result='+0ᐎᐐᐊ')
    zfill_verifier.verify('-ᐎᐐᐊ', 5, expected_result='-0ᐎᐐᐊ')
