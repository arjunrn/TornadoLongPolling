import tornado.web
import tornado.httpserver
import tornado.ioloop
import tornado.options
from lpmix import LongPollMixin
import uuid
import json

class RootHandler(tornado.web.RequestHandler):
	def get(self):
		self.render("index.html")

class NewMessageHandler(tornado.web.RequestHandler, LongPollMixin):
    def post(self):
        message = {
            "id" : str(uuid.uuid4()),
            "body" : self.get_argument("body")
        }
        try:
            self.new_message(message)
        except:
            self.write(json.dumps({"result":False}, indent=4))
            return
        self.write(json.dumps({"result":True}, indent=4))

class MessageFormHandler(tornado.web.RequestHandler):
	def get(self):
		self.render("message_form.html")


class LongPollHandler(tornado.web.RequestHandler, LongPollMixin):
    @tornado.web.asynchronous
    def get(self):
        self.wait_for_message(self.on_new_message)
    
    def on_new_message(self, message):
        if self.request.connection.stream.closed():
            return
        self.write(json.dumps(message, indent=4))
        self.finish()
    
    def on_connection_close(self):
        print "On Connection Closed"
        self.cancel_wait(self.on_new_message)
        

class Application(tornado.web.Application):
	def __init__(self):
		handlers = [
			(r'^/$', RootHandler),
            (r'^/long-poll/$', LongPollHandler),
            (r'^/new-message/$', NewMessageHandler),
            (r'^/message-form/$', MessageFormHandler),
		]
		
		settings = {
			'template_path' : 'templates',
			'static_path' : 'static',
            'debug':True,
		}
		
		tornado.web.Application.__init__(self, handlers, **settings)
		
if __name__ == '__main__':
    tornado.options.parse_command_line()
    app = Application()
    server = tornado.httpserver.HTTPServer(app)
    server.listen(8000)
    tornado.ioloop.IOLoop.instance().start()
