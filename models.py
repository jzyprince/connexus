import datetime
import operator
import threading

from google.appengine.ext import ndb

from utils import ModelUtils


class Image(ndb.Model):
    """Models an image which is linked via Stream
    """
    data = ndb.BlobProperty()
    comment = ndb.StringProperty()
    lat = ndb.FloatProperty()
    lng = ndb.FloatProperty()
    date = ndb.DateTimeProperty(auto_now_add=True, required=True)


class Meta(ndb.Model):
    email_duration = ndb.IntegerProperty()
    cached_tags = ndb.StringProperty(repeated=True)

    @classmethod
    def get_meta(cls):
        meta = Meta.get_by_id('meta')
        if not meta:
            meta = Meta(id='meta')
        return meta


class Stream(ModelUtils, ndb.Model):
    """Models a Stream which contains many images
    """
    image_ids = ndb.KeyProperty(repeated=True, kind=Image)
    tags = ndb.StringProperty(repeated=True)
    cover_url = ndb.StringProperty()
    view_count = ndb.IntegerProperty(default=0)
    date = ndb.DateTimeProperty(auto_now_add=True, required=True)
    _use_memcache = False
    _use_cache = False
    _config_lock = threading.Lock()

    @classmethod
    def append_image(cls, stream_id, image_id):
        with cls._config_lock:
            stream = Stream.get_by_id(stream_id)
            stream.image_ids.append(image_id)
            stream.put()
            print stream.image_ids

    def check_tags(self, query):
        for tag in self.tags:
            if query in tag:
                return True
        return False

    def last_image_date(self):
        dates = [image.get().date for image in self.image_ids]
        if len(dates) == 0:
            return None
        return sorted(dates, reverse=True)[0]

    def image_count(self):
        return len(self.image_ids)


class User(ModelUtils, ndb.Model):
    owned_ids = ndb.KeyProperty(repeated=True, kind=Stream)
    subscribed_ids = ndb.KeyProperty(repeated=True, kind=Stream)
    date = ndb.DateTimeProperty(auto_now_add=True, required=True)

    def is_subscribed(self, stream_name):
        return stream_name in [x.id() for x in self.subscribed_ids]

    def is_owned(self, stream_name):
        return stream_name in [x.id() for x in self.owned_ids]

    def owned_id_details(self):
        return [{'name': stream.id(),
                 'last_date': stream.get().last_image_date(),
                 'image_count': stream.get().image_count()}
                for stream in self.owned_ids]

    def subscribed_images(self):
        images = []
        for stream in self.subscribed_ids:
            if stream.get():
                for image_id in stream.get().image_ids:
                    images.append(Image.get_by_id(int(image_id.id())))
        sorted_images = sorted(images, key=lambda img: img.date, reverse=True)
        return [img.key.id() for img in sorted_images]

    def subscribed_id_details(self):
        return [{'name': stream.id(),
                 'last_date': stream.get().last_image_date(),
                 'view_count': stream.get().view_count,
                 'image_count': stream.get().image_count()}
                for stream in self.subscribed_ids if stream.get()]


class View(ndb.Model):
    stream_id = ndb.KeyProperty(kind=Stream)
    date = ndb.DateTimeProperty(auto_now_add=True, required=True)


class Leaderboard(ndb.Model):
    stream_id = ndb.KeyProperty(kind=Stream)
    view_count = ndb.IntegerProperty()
    interval = ndb.IntegerProperty(default=0)

    @classmethod
    def refresh(cls, duration=5):
        duration = int(duration)
        date_limit = datetime.datetime.now() - datetime.timedelta(
            minutes=duration)
        views = View.query(View.date > date_limit).fetch()
        stream_freq = {}
        for view in views:
            stream_freq[view.stream_id] = stream_freq.get(
                view.stream_id, 0) + 1

        sorted_streams = sorted(
            stream_freq.items(), key=operator.itemgetter(1), reverse=True)

        ndb.delete_multi(Leaderboard.query().fetch(keys_only=True))

        for stream in sorted_streams:
            if stream[0].get():
                Leaderboard(stream_id=stream[0], view_count=stream[1],
                            interval=duration).put()
