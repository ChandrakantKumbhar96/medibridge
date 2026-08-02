"""Groq function-calling tool schemas (OpenAI-compatible) offered to the FAQ
assistant. RAG retrieval covers *what a feature is*; tools cover *what the
current numbers are* - the two are complementary, not overlapping, so the
system prompt in app/llm/prompts.py tells the model when to reach for one.

Adding a tool means a schema here and a handler in app/tools/handlers.py -
the name is the link between the two, checked by dispatch().
"""

GET_POLICIES = {
    "type": "function",
    "function": {
        "name": "get_policies",
        "description": (
            "Get MediBridge's current cancellation, reschedule, no-show and "
            "follow-up policy numbers (hours, percentages, limits). Call this "
            "whenever a patient asks how much refund they get, how many hours "
            "before an appointment counts as free cancellation, how many times "
            "they can reschedule, or whether a follow-up is free - never guess "
            "or quote a number from memory."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

TOOLS = [GET_POLICIES]
