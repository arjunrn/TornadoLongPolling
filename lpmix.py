import logging

class LongPollMixin(object):
    waiters = set()
    
    def wait_for_message(self, callback):
        cls = LongPollMixin
        cls.waiters.add(callback)
    
    def cancel_wait(self, callback):
        cls = LongPollMixin
        cls.waiters.remove(callback)
        
    def new_message(self, messages):
        cls = LongPollMixin
        logging.info("Sending new message to %r listeners", len(cls.waiters))
        for callback in cls.waiters:
            try:
                callback(messages)
            except:
                logging.error("Error in waiter callback", exc_info=True)
            cls.waiters = set()
                
