class ScoreDirector:
    """
    The `ScoreDirector` holds the working solution and calculates the `Score` for it.
    """
    def __init__(self, delegate):
        self._delegate = delegate

    def after_entity_added(self, entity) -> None:
        self._delegate.afterEntityAdded(entity)
    
    def after_entity_removed(self, entity) -> None:
        self._delegate.afterEntityRemoved(entity)
    
    def after_list_variable_changed(self, entity, variable_name: str, start: int, end: int) -> None:
        self._delegate.afterListVariableChanged(entity, variable_name, start, end)
    
    def after_list_variable_element_assigned(self, entity, variable_name: str, element) -> None:
        self._delegate.afterListVariableElementAssigned(entity, variable_name, element)
    
    def after_list_variable_element_unassigned(self, entity, variable_name: str, element) -> None:
        self._delegate.afterListVariableElementUnassigned(entity, variable_name, element)
    
    def after_problem_fact_added(self, entity) -> None:
        self._delegate.afterProblemFactAdded(entity)
    
    def after_problem_fact_removed(self, entity) -> None:
        self._delegate.afterProblemFactRemoved(entity)
    
    def after_problem_property_changed(self, entity) -> None:
        self._delegate.afterProblemPropertyChanged(entity)
    
    def after_variable_changed(self, entity, variable_name: str) -> None:
        self._delegate.afterVariableChanged(entity, variable_name)
    
    def before_entity_added(self, entity) -> None:
        self._delegate.beforeEntityAdded(entity)
    
    def before_entity_removed(self, entity) -> None:
        self._delegate.beforeEntityRemoved(entity)
    
    def before_list_variable_changed(self, entity, variable_name: str, start: int, end: int) -> None:
        self._delegate.beforeListVariableChanged(entity, variable_name, start, end)
    
    def before_list_variable_element_assigned(self, entity, variable_name: str, element) -> None:
        self._delegate.beforeListVariableElementAssigned(entity, variable_name, element)
    
    def before_list_variable_element_unassigned(self, entity, variable_name: str, element) -> None:
        self._delegate.beforeListVariableElementUnassigned(entity, variable_name, element)
    
    def before_problem_fact_added(self, entity) -> None:
        self._delegate.beforeProblemFactAdded(entity)
    
    def before_problem_fact_removed(self, entity) -> None:
        self._delegate.beforeProblemFactRemoved(entity)
    
    def before_problem_property_changed(self, entity) -> None:
        self._delegate.beforeProblemPropertyChanged(entity)
    
    def before_variable_changed(self, entity, variable_name: str) -> None:
        self._delegate.beforeVariableChanged(entity, variable_name)
    
    def get_working_solution(self):
        """
        The `planning_solution` that is used to calculate the `Score`.
        Because a `Score` is best calculated incrementally (by deltas),
        the ScoreDirector needs to be notified when its working solution changes.
        """
        return self._delegate.getWorkingSolution()
    
    def look_up_working_object(self, working_object):
        """
        Translates an entity or fact instance (often from another Thread)
        to this `ScoreDirector`'s internal working instance.
        Useful for move rebasing and in a `ProblemChange`.
        Matching uses a `PlanningId` by default.
        """
        return self._delegate.lookUpWorkingObject(working_object)
    
    def look_up_working_object_or_return_none(self, working_object):
        """
        As defined by `look_up_working_object`,
        but doesn't fail fast if no `working_object` was ever added for the `external_object`.
        It's recommended to use `look_up_working_object` instead, especially in move rebasing code.
        """
        return self._delegate.lookUpWorkingObject(working_object)
    
    def trigger_variable_listeners(self) -> None:
        self._delegate.triggerVariableListeners()


__all__ = ['ScoreDirector']
