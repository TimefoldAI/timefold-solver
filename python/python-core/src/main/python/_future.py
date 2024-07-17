from typing import Awaitable, TypeVar, TYPE_CHECKING

if TYPE_CHECKING:
    from java.util.concurrent import Future as JavaFuture


Result = TypeVar('Result')


class JavaFutureAwaitable(Awaitable[Result]):
    _future: 'JavaFuture[Result]'

    def __init__(self, future: 'JavaFuture[Result]') -> None:
        self._future = future

    def __await__(self) -> Result:
        return self

    def __iter__(self):
        return self

    def __next__(self):
        raise StopIteration(self._future.get())


def wrap_future(future: 'JavaFuture[Result]') -> Awaitable[Result]:
    return JavaFutureAwaitable(future)


__all__ = ['wrap_future']
